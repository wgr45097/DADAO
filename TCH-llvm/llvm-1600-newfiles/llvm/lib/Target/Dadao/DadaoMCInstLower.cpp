//=-- DadaoMCInstLower.cpp - Convert Dadao MachineInstr to an MCInst --------=//
//
// Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
// See https://llvm.org/LICENSE.txt for license information.
// SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
//
//===----------------------------------------------------------------------===//
//
// This file contains code to lower Dadao MachineInstrs to their corresponding
// MCInst records.
//
//===----------------------------------------------------------------------===//

#include "DadaoMCInstLower.h"

#include "MCTargetDesc/DadaoBaseInfo.h"
#include "MCTargetDesc/DadaoMCExpr.h"
#include "llvm/ADT/SmallString.h"
#include "llvm/CodeGen/AsmPrinter.h"
#include "llvm/CodeGen/MachineBasicBlock.h"
#include "llvm/CodeGen/MachineInstr.h"
#include "llvm/IR/Constants.h"
#include "llvm/MC/MCAsmInfo.h"
#include "llvm/MC/MCContext.h"
#include "llvm/MC/MCExpr.h"
#include "llvm/MC/MCInst.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/raw_ostream.h"

using namespace llvm;

MCSymbol *
DadaoMCInstLower::GetGlobalAddressSymbol(const MachineOperand &MO) const {
  return Printer.getSymbol(MO.getGlobal());
}

MCSymbol *
DadaoMCInstLower::GetBlockAddressSymbol(const MachineOperand &MO) const {
  return Printer.GetBlockAddressSymbol(MO.getBlockAddress());
}

MCSymbol *
DadaoMCInstLower::GetExternalSymbolSymbol(const MachineOperand &MO) const {
  return Printer.GetExternalSymbolSymbol(MO.getSymbolName());
}

MCSymbol *DadaoMCInstLower::GetJumpTableSymbol(const MachineOperand &MO) const {
  SmallString<256> Name;
  raw_svector_ostream(Name) << Printer.MAI->getPrivateGlobalPrefix() << "JTI"
                            << Printer.getFunctionNumber() << '_'
                            << MO.getIndex();
  // Create a symbol for the name.
  return Ctx.getOrCreateSymbol(Name.str());
}

MCSymbol *
DadaoMCInstLower::GetConstantPoolIndexSymbol(const MachineOperand &MO) const {
  SmallString<256> Name;
  raw_svector_ostream(Name) << Printer.MAI->getPrivateGlobalPrefix() << "CPI"
                            << Printer.getFunctionNumber() << '_'
                            << MO.getIndex();
  // Create a symbol for the name.
  return Ctx.getOrCreateSymbol(Name.str());
}

MCOperand DadaoMCInstLower::LowerSymbolOperand(const MachineOperand &MO,
                                               MCSymbol *Sym) const {
  DadaoMCExpr::VariantKind Kind;

  switch (MO.getTargetFlags()) {
  case DadaoII::MO_NO_FLAG:
    Kind = DadaoMCExpr::VK_Dadao_None;
    break;
  case DadaoII::MO_ABS_HI:
    Kind = DadaoMCExpr::VK_Dadao_ABS_HI;
    break;
  case DadaoII::MO_ABS_LO:
    Kind = DadaoMCExpr::VK_Dadao_ABS_LO;
    break;
  case DadaoII::MO_ABS:
    Kind = DadaoMCExpr::VK_Dadao_None;
    break;
  default:
    llvm_unreachable("Unknown target flag on GV operand");
  }

  const MCExpr *Expr =
      MCSymbolRefExpr::create(Sym, MCSymbolRefExpr::VK_None, Ctx);
  if (!MO.isJTI() && MO.getOffset()) // What's this?
    Expr = MCBinaryExpr::createAdd(
        Expr, MCConstantExpr::create(MO.getOffset(), Ctx), Ctx);
  Expr = DadaoMCExpr::create(Kind, Expr, Ctx);
  return MCOperand::createExpr(Expr);
}

void DadaoMCInstLower::Lower(const MachineInstr *MI, MCInst &OutMI) const {
  // Maybe lower an abs relocation here? Will assembler ever reach here?
  OutMI.setOpcode(MI->getOpcode());

  for (const MachineOperand &MO : MI->operands()) {
    MCOperand MCOp;
    switch (MO.getType()) {
    case MachineOperand::MO_Register:
      // Ignore all implicit register operands.
      if (MO.isImplicit())
        continue;
      MCOp = MCOperand::createReg(MO.getReg());
      break;
    case MachineOperand::MO_Immediate:
      MCOp = MCOperand::createImm(MO.getImm());
      break;
    case MachineOperand::MO_MachineBasicBlock:
      MCOp = MCOperand::createExpr(
          MCSymbolRefExpr::create(MO.getMBB()->getSymbol(), Ctx));
      break;
    case MachineOperand::MO_RegisterMask:
      continue;
    case MachineOperand::MO_GlobalAddress:
      MCOp = LowerSymbolOperand(MO, GetGlobalAddressSymbol(MO));
      break;
    case MachineOperand::MO_BlockAddress:
      MCOp = LowerSymbolOperand(MO, GetBlockAddressSymbol(MO));
      break;
    case MachineOperand::MO_ExternalSymbol:
      MCOp = LowerSymbolOperand(MO, GetExternalSymbolSymbol(MO));
      break;
    case MachineOperand::MO_JumpTableIndex:
      MCOp = LowerSymbolOperand(MO, GetJumpTableSymbol(MO));
      break;
    case MachineOperand::MO_ConstantPoolIndex:
      MCOp = LowerSymbolOperand(MO, GetConstantPoolIndexSymbol(MO));
      break;
    default:
      MI->print(errs());
      llvm_unreachable("unknown operand type");
    }

    OutMI.addOperand(MCOp);
  }
}
