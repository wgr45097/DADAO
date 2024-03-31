//===-- DadaoFrameLowering.cpp - Dadao Frame Information ------------------===//
//
// Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
// See https://llvm.org/LICENSE.txt for license information.
// SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
//
//===----------------------------------------------------------------------===//
//
// This file contains the Dadao implementation of TargetFrameLowering class.
//
//===----------------------------------------------------------------------===//

#include "DadaoFrameLowering.h"

#include "DadaoInstrInfo.h"
#include "DadaoSubtarget.h"
#include "DadaoMachineFunctionInfo.h"
#include "llvm/CodeGen/MachineFrameInfo.h"
#include "llvm/CodeGen/MachineFunction.h"
#include "llvm/CodeGen/MachineInstrBuilder.h"
#include "llvm/CodeGen/MachineRegisterInfo.h"
#include "llvm/IR/Function.h"

using namespace llvm;

// Determines the size of the frame and maximum call frame size.
void DadaoFrameLowering::determineFrameLayout(MachineFunction &MF) const {
  MachineFrameInfo &MFI = MF.getFrameInfo();
  DadaoMachineFunctionInfo *DadaoMFI = MF.getInfo<DadaoMachineFunctionInfo>();
  const DadaoRegisterInfo *LRI = STI.getRegisterInfo();

  // Get the number of bytes to allocate from the FrameInfo.
  unsigned FrameSize = MFI.getStackSize();

  // Get the alignment.
  Align StackAlign =
      LRI->hasStackRealignment(MF) ? MFI.getMaxAlign() : getStackAlign();

  // Get the maximum call frame size of all the calls.
  unsigned MaxCallFrameSize = MFI.getMaxCallFrameSize();

  // If we have dynamic alloca then MaxCallFrameSize needs to be aligned so
  // that allocations will be aligned.
  if (MFI.hasVarSizedObjects())
    MaxCallFrameSize = alignTo(MaxCallFrameSize, StackAlign);

  // Update maximum call frame size.
  MFI.setMaxCallFrameSize(MaxCallFrameSize);

  // Include call frame size in total.
  if (!(hasReservedCallFrame(MF) && MFI.adjustsStack()))
    FrameSize += MaxCallFrameSize;

  // Make sure the frame is aligned.
  FrameSize = alignTo(FrameSize, StackAlign);

  // Update frame info.
  MFI.setStackSize(FrameSize);
}

// Iterates through each basic block in a machine function and replaces
// ADJDYNALLOC pseudo instructions with a Dadao:ADDI with the
// maximum call frame size as the immediate.
void DadaoFrameLowering::replaceAdjDynAllocPseudo(MachineFunction &MF) const {
  const DadaoInstrInfo &LII =
      *static_cast<const DadaoInstrInfo *>(STI.getInstrInfo());
  unsigned MaxCallFrameSize = MF.getFrameInfo().getMaxCallFrameSize();

  for (MachineBasicBlock &MBB : MF) {
    for (MachineInstr &MI : llvm::make_early_inc_range(MBB)) {
      if (MI.getOpcode() == Dadao::ADJDYNALLOC) {
        DebugLoc DL = MI.getDebugLoc();
        Register Dst = MI.getOperand(0).getReg();
        Register Src = MI.getOperand(1).getReg();

        BuildMI(MBB, MI, DL, LII.get(Dadao::ADDI_RRII), Dst)
            .addReg(Src)
            .addImm(MaxCallFrameSize);
        MI.eraseFromParent();
      }
    }
  }
}

// Generates the following sequence for function entry:
//   st %fp,-4[*%sp]        !push old FP
//   add %sp,8,%fp          !generate new FP
//   sub %sp,0x4,%sp        !allocate stack space (as needed)
void DadaoFrameLowering::emitPrologue(MachineFunction &MF,
                                      MachineBasicBlock &MBB) const {
  assert(&MF.front() == &MBB && "Shrink-wrapping not yet supported");

  MachineFrameInfo &MFI = MF.getFrameInfo();
  DadaoMachineFunctionInfo *FuncInfo = MF.getInfo<DadaoMachineFunctionInfo>();
  const DadaoInstrInfo &LII =
      *static_cast<const DadaoInstrInfo *>(STI.getInstrInfo());
  MachineBasicBlock::iterator MBBI = MBB.begin();

  // Debug location must be unknown since the first debug location is used
  // to determine the end of the prologue.
  DebugLoc DL;

  // Determine the correct frame layout
  determineFrameLayout(MF);

  // FIXME: This appears to be overallocating.  Needs investigation.
  // Get the number of bytes to allocate from the FrameInfo.
  unsigned StackSize = MFI.getStackSize();

  // Push old FP
  // st %fp,-8[*%sp]
  BuildMI(MBB, MBBI, DL, LII.get(Dadao::STRB_RRII))
      .addReg(Dadao::RBFP)
      .addReg(Dadao::RBSP)
      .addImm(FuncInfo->getSpOffset())
      .setMIFlag(MachineInstr::FrameSetup);

  // Generate new FP
  // add %sp,0,%fp
  BuildMI(MBB, MBBI, DL, LII.get(Dadao::ADDI_RB_RRII), Dadao::RBFP)
      .addReg(Dadao::RBSP)
      .addImm(0)
      .setMIFlag(MachineInstr::FrameSetup);

  // Allocate space on the stack if needed
  // sub %sp,StackSize,%sp
  if (StackSize != 0) {
    BuildMI(MBB, MBBI, DL, LII.get(Dadao::ADDI_RB_RRII), Dadao::RBSP)
        .addReg(Dadao::RBSP)
        .addImm(-(long long)StackSize)
        .setMIFlag(MachineInstr::FrameSetup);
  }

  // Replace ADJDYNANALLOC
  if (MFI.hasVarSizedObjects())
    replaceAdjDynAllocPseudo(MF);
}

MachineBasicBlock::iterator DadaoFrameLowering::eliminateCallFramePseudoInstr(
    MachineFunction & /*MF*/, MachineBasicBlock &MBB,
    MachineBasicBlock::iterator I) const {
  // Discard ADJCALLSTACKDOWN, ADJCALLSTACKUP instructions.
  return MBB.erase(I);
}

// The function epilogue should not depend on the current stack pointer!
// It should use the frame pointer only.  This is mandatory because
// of alloca; we also take advantage of it to omit stack adjustments
// before returning.
//
// Note that when we go to restore the preserved register values we must
// not try to address their slots by using offsets from the stack pointer.
// That's because the stack pointer may have been moved during the function
// execution due to a call to alloca().  Rather, we must restore all
// preserved registers via offsets from the frame pointer value.
//
// Note also that when the current frame is being "popped" (by adjusting
// the value of the stack pointer) on function exit, we must (for the
// sake of alloca) set the new value of the stack pointer based upon
// the current value of the frame pointer.  We can't just add what we
// believe to be the (static) frame size to the stack pointer because
// if we did that, and alloca() had been called during this function,
// we would end up returning *without* having fully deallocated all of
// the space grabbed by alloca.  If that happened, and a function
// containing one or more alloca() calls was called over and over again,
// then the stack would grow without limit!
//
// RET is lowered to
//      ld -4[%fp],%pc  # modify %pc (two delay slots)
// as the return address is in the stack frame and mov to pc is allowed.
// emitEpilogue emits
//      mov %fp,%sp     # restore the stack pointer
//      ld -8[%fp],%fp  # restore the caller's frame pointer
// before RET and the delay slot filler will move RET such that these
// instructions execute in the delay slots of the load to PC.
void DadaoFrameLowering::emitEpilogue(MachineFunction & MF,
                                      MachineBasicBlock &MBB) const {
  MachineBasicBlock::iterator MBBI = MBB.getLastNonDebugInstr();
  DadaoMachineFunctionInfo *FuncInfo = MF.getInfo<DadaoMachineFunctionInfo>();
  const DadaoInstrInfo &LII =
      *static_cast<const DadaoInstrInfo *>(STI.getInstrInfo());
  DebugLoc DL = MBBI->getDebugLoc();

  // Restore the stack pointer using the callee's frame pointer value.
  BuildMI(MBB, MBBI, DL, LII.get(Dadao::ADDI_RB_RRII), Dadao::RBSP)
      .addReg(Dadao::RBFP)
      .addImm(0);

  // Restore the frame pointer from the stack.
  BuildMI(MBB, MBBI, DL, LII.get(Dadao::LDRB_RRII), Dadao::RBFP)
      .addReg(Dadao::RBFP)
      .addImm(FuncInfo->getSpOffset());
}

void DadaoFrameLowering::determineCalleeSaves(MachineFunction &MF,
                                              BitVector &SavedRegs,
                                              RegScavenger *RS) const {
  TargetFrameLowering::determineCalleeSaves(MF, SavedRegs, RS);

  MachineFrameInfo &MFI = MF.getFrameInfo();
  DadaoMachineFunctionInfo *FuncInfo = MF.getInfo<DadaoMachineFunctionInfo>();
  const DadaoRegisterInfo *LRI =
      static_cast<const DadaoRegisterInfo *>(STI.getRegisterInfo());
  int Offset = -FuncInfo->getVarArgsSaveSize()-8;
  FuncInfo->setSpOffset(Offset);

  // Reserve 8 bytes for the saved RBSP
  MFI.CreateFixedObject(8, Offset, true);
  Offset -= 8;

  if (LRI->hasBasePointer(MF)) {
    MFI.CreateFixedObject(8, Offset, true);
    SavedRegs.reset(LRI->getBaseRegister());
  }
}
