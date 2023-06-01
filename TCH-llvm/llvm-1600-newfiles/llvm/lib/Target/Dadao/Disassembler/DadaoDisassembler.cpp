//===- DadaoDisassembler.cpp - Disassembler for Dadao -----------*- C++ -*-===//
//
// Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
// See https://llvm.org/LICENSE.txt for license information.
// SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
//
//===----------------------------------------------------------------------===//
//
// This file is part of the Dadao Disassembler.
//
//===----------------------------------------------------------------------===//

#include "DadaoDisassembler.h"

#include "DadaoAluCode.h"
#include "DadaoCondCode.h"
#include "DadaoInstrInfo.h"
#include "TargetInfo/DadaoTargetInfo.h"
#include "llvm/MC/MCDecoderOps.h"
#include "llvm/MC/MCInst.h"
#include "llvm/MC/MCSubtargetInfo.h"
#include "llvm/MC/TargetRegistry.h"
#include "llvm/Support/MathExtras.h"

using namespace llvm;

typedef MCDisassembler::DecodeStatus DecodeStatus;

static MCDisassembler *createDadaoDisassembler(const Target & /*T*/,
                                               const MCSubtargetInfo &STI,
                                               MCContext &Ctx) {
  return new DadaoDisassembler(STI, Ctx);
}

extern "C" LLVM_EXTERNAL_VISIBILITY void LLVMInitializeDadaoDisassembler() {
  // Register the disassembler
  TargetRegistry::RegisterMCDisassembler(getTheDadaoTarget(),
                                         createDadaoDisassembler);
}

DadaoDisassembler::DadaoDisassembler(const MCSubtargetInfo &STI, MCContext &Ctx)
    : MCDisassembler(STI, Ctx) {}

// Forward declare because the autogenerated code will reference this.
// Definition is further down.
static DecodeStatus DecodeGPRDRegisterClass(MCInst &Inst, unsigned RegNo,
                                            uint64_t Address,
                                            const MCDisassembler *Decoder);

static DecodeStatus DecodeGPRBRegisterClass(MCInst &Inst, unsigned RegNo,
                                            uint64_t Address,
                                            const MCDisassembler *Decoder);

static DecodeStatus decodeRRIIMemoryValue(MCInst &Inst, unsigned Insn,
                                        uint64_t Address,
                                        const MCDisassembler *Decoder);

static DecodeStatus decodeRRRIMemoryValue(MCInst &Inst, unsigned Insn,
                                        uint64_t Address,
                                        const MCDisassembler *Decoder);

static DecodeStatus decodeBranch(MCInst &Inst, unsigned Insn, uint64_t Address,
                                 const MCDisassembler *Decoder);

#include "DadaoGenDisassemblerTables.inc"

static DecodeStatus readInstruction32(ArrayRef<uint8_t> Bytes, uint64_t &Size,
                                      uint32_t &Insn) {
  // We want to read exactly 4 bytes of data.
  if (Bytes.size() < 4) {
    Size = 0;
    return MCDisassembler::Fail;
  }

  // Encoded as big-endian 32-bit word in the stream.
  Insn =
      (Bytes[0] << 24) | (Bytes[1] << 16) | (Bytes[2] << 8) | (Bytes[3] << 0);

  return MCDisassembler::Success;
}

static void PostOperandDecodeAdjust(MCInst &Instr, uint32_t Insn) {
  unsigned AluOp = LPAC::ADD;
  // Fix up for pre and post operations.
  int PqShift = -1;

  if (PqShift != -1) {
    unsigned PQ = (Insn >> PqShift) & 0x3;
    switch (PQ) {
    case 0x0:
      if (Instr.getOperand(2).isReg()) {
        Instr.getOperand(2).setReg(Dadao::RDZERO);
      }
      if (Instr.getOperand(2).isImm())
        Instr.getOperand(2).setImm(0);
      break;
    case 0x1:
      AluOp = LPAC::makePostOp(AluOp);
      break;
    case 0x2:
      break;
    case 0x3:
      AluOp = LPAC::makePreOp(AluOp);
      break;
    }
    Instr.addOperand(MCOperand::createImm(AluOp));
  }
}

DecodeStatus
DadaoDisassembler::getInstruction(MCInst &Instr, uint64_t &Size,
                                  ArrayRef<uint8_t> Bytes, uint64_t Address,
                                  raw_ostream & /*CStream*/) const {
  uint32_t Insn;

  DecodeStatus Result = readInstruction32(Bytes, Size, Insn);

  if (Result == MCDisassembler::Fail)
    return MCDisassembler::Fail;

  // Call auto-generated decoder function
  Result =
      decodeInstruction(DecoderTableDadao32, Instr, Insn, Address, this, STI);

  if (Result != MCDisassembler::Fail) {
    PostOperandDecodeAdjust(Instr, Insn);
    Size = 4;
    return Result;
  }

  return MCDisassembler::Fail;
}

static const unsigned GPRDDecoderTable[] = {
    Dadao::RD0,  Dadao::RD1,  Dadao::RD2,  Dadao::RD3,  Dadao::RD4,  Dadao::RD5,  Dadao::RD6,  Dadao::RD7,
    Dadao::RD8,  Dadao::RD9,  Dadao::RD10, Dadao::RD11, Dadao::RD12, Dadao::RD13, Dadao::RD14, Dadao::RD15,
    Dadao::RD16, Dadao::RD17, Dadao::RD18, Dadao::RD19, Dadao::RD20, Dadao::RD21, Dadao::RD22, Dadao::RD23,
    Dadao::RD24, Dadao::RD25, Dadao::RD26, Dadao::RD27, Dadao::RD28, Dadao::RD29, Dadao::RD30, Dadao::RD31,
    Dadao::RD32, Dadao::RD33, Dadao::RD34, Dadao::RD35, Dadao::RD36, Dadao::RD37, Dadao::RD38, Dadao::RD39,
    Dadao::RD40, Dadao::RD41, Dadao::RD42, Dadao::RD43, Dadao::RD44, Dadao::RD45, Dadao::RD46, Dadao::RD47,
    Dadao::RD48, Dadao::RD49, Dadao::RD50, Dadao::RD51, Dadao::RD52, Dadao::RD53, Dadao::RD54, Dadao::RD55,
    Dadao::RD56, Dadao::RD57, Dadao::RD58, Dadao::RD59, Dadao::RD60, Dadao::RD61, Dadao::RD62, Dadao::RD63};

DecodeStatus DecodeGPRDRegisterClass(MCInst &Inst, unsigned RegNo,
                                    uint64_t /*Address*/,
                                    const MCDisassembler * /*Decoder*/) {
  if (RegNo > 63)
    return MCDisassembler::Fail;

  unsigned Reg = GPRDDecoderTable[RegNo];
  Inst.addOperand(MCOperand::createReg(Reg));
  return MCDisassembler::Success;
}

static const unsigned GPRBDecoderTable[] = {
    Dadao::RB0,  Dadao::RB1,  Dadao::RB2,  Dadao::RB3,  Dadao::RB4,  Dadao::RB5,  Dadao::RB6,  Dadao::RB7,
    Dadao::RB8,  Dadao::RB9,  Dadao::RB10, Dadao::RB11, Dadao::RB12, Dadao::RB13, Dadao::RB14, Dadao::RB15,
    Dadao::RB16, Dadao::RB17, Dadao::RB18, Dadao::RB19, Dadao::RB20, Dadao::RB21, Dadao::RB22, Dadao::RB23,
    Dadao::RB24, Dadao::RB25, Dadao::RB26, Dadao::RB27, Dadao::RB28, Dadao::RB29, Dadao::RB30, Dadao::RB31,
    Dadao::RB32, Dadao::RB33, Dadao::RB34, Dadao::RB35, Dadao::RB36, Dadao::RB37, Dadao::RB38, Dadao::RB39,
    Dadao::RB40, Dadao::RB41, Dadao::RB42, Dadao::RB43, Dadao::RB44, Dadao::RB45, Dadao::RB46, Dadao::RB47,
    Dadao::RB48, Dadao::RB49, Dadao::RB50, Dadao::RB51, Dadao::RB52, Dadao::RB53, Dadao::RB54, Dadao::RB55,
    Dadao::RB56, Dadao::RB57, Dadao::RB58, Dadao::RB59, Dadao::RB60, Dadao::RB61, Dadao::RB62, Dadao::RB63};

DecodeStatus DecodeGPRBRegisterClass(MCInst &Inst, unsigned RegNo,
                                    uint64_t /*Address*/,
                                    const MCDisassembler * /*Decoder*/) {
  if (RegNo > 63)
    return MCDisassembler::Fail;

  unsigned Reg = GPRBDecoderTable[RegNo];
  Inst.addOperand(MCOperand::createReg(Reg));
  return MCDisassembler::Success;
}

static DecodeStatus decodeRRIIMemoryValue(MCInst &Inst, unsigned Insn,
                                        uint64_t Address,
                                        const MCDisassembler *Decoder) {
  // RRII memory values encoded using 18 bits:
  //   6 bit register, 12 bit constant
  unsigned Register = (Insn >> 12) & 0x3f;
  Inst.addOperand(MCOperand::createReg(GPRDDecoderTable[Register]));
  unsigned Offset = (Insn & 0xfff);
  Inst.addOperand(MCOperand::createImm(SignExtend64<12>(Offset)));

  return MCDisassembler::Success;
}

static DecodeStatus decodeRRRIMemoryValue(MCInst &Inst, unsigned Insn,
                                        uint64_t Address,
                                        const MCDisassembler *Decoder) {
  // RRRI memory values encoded using 18 bits:
  //   6 bit RB register, 6 bit RD register, 6 bit Imm6
  unsigned Register = (Insn >> 12) & 0x3f;
  Inst.addOperand(MCOperand::createReg(GPRBDecoderTable[Register]));
  Register = (Insn >> 6) & 0x3f;
  Inst.addOperand(MCOperand::createReg(GPRDDecoderTable[Register]));

  return MCDisassembler::Success;
}

static bool tryAddingSymbolicOperand(int64_t Value, bool IsBranch,
                                     uint64_t Address, uint64_t Offset,
                                     uint64_t Width, MCInst &MI,
                                     const MCDisassembler *Decoder) {
  return Decoder->tryAddingSymbolicOperand(MI, Value, Address, IsBranch, Offset,
                                           Width, /*InstSize=*/0);
}

static DecodeStatus decodeBranch(MCInst &MI, unsigned Insn, uint64_t Address,
                                 const MCDisassembler *Decoder) {
  if (!tryAddingSymbolicOperand(Insn + Address, false, Address, 2, 23, MI,
                                Decoder))
    MI.addOperand(MCOperand::createImm(Insn));
  return MCDisassembler::Success;
}
