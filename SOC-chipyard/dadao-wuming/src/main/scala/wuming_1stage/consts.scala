//**************************************************************************
// RISCV Processor Constants
//--------------------------------------------------------------------------
//
// Christopher Celio
// 2011 May 28

package wuming.stage1
package constants
{

import chisel3._
import chisel3.util._


trait WumingProcConstants
{
   // for debugging, print out the commit information.
   // can be compared against the riscv-isa-run Spike ISA simulator's commit logger.
   val PRINT_COMMIT_LOG = false
}

trait ScalarOpConstants
{
   //************************************
   // Control Signals

   val Y      = true.B
   val N      = false.B

   // PC Select Signal
   val PC_4   = 0.asUInt(3.W)  // PC + 4
   val PC_BR12  = 1.asUInt(3.W)  // branch_target for imms12
   val PC_BR18  = 5.asUInt(3.W)  // branch_target for imms18
   val PC_EXC = 4.asUInt(3.W)  // exception
   val PC_JMPI  = 6.asUInt(3.W)  // jump_iiii_target
   val PC_JMPR  = 7.asUInt(3.W)  // jump_rrii_target
   val PC_RA    = 2.asUInt(3.W)  // pop from RA regfile

   // Branch Type
   val BR_X   = 0.asUInt(4.W)  // Next
   val BR_NE  = 1.asUInt(4.W)  // Branch on NotEqual
   val BR_EQ  = 2.asUInt(4.W)  // Branch on Equal
   val BR_N   = 3.asUInt(4.W)  // Branch on Negative
   val BR_NN  = 4.asUInt(4.W)  // Branch on Not Negative
   val BR_Z   = 5.asUInt(4.W)  // Branch on Zero
   val BR_NZ  = 6.asUInt(4.W)  // Branch on Not Zero
   val BR_P   = 9.asUInt(4.W)  // Branch on Positive
   val BR_NP  = 10.asUInt(4.W) // Branch on Not Positive
   val BR_JMPI  = 11.asUInt(4.W)  // Jump iiii
   val BR_JMPR  = 12.asUInt(4.W)  // Jump rrii
   val BR_RET   = 13.asUInt(4.W)  // Return

   // Register group select
   val REG_RD  = 0.asUInt(2.W)
   val REG_RB  = 1.asUInt(2.W)
   val REG_RA  = 2.asUInt(2.W)
   val REG_RF  = 3.asUInt(2.W)
   val REG_X   = 0.asUInt(2.W)

   // Operand 1 Select Signal
   val OP1_RDHC = 4.asUInt(4.W)
   val OP1_RBHC = 5.asUInt(4.W)
   val OP1_RDHA = 6.asUInt(4.W)
   val OP1_RBHA = 7.asUInt(4.W)
   val OP1_RBHB = 8.asUInt(4.W)
   val OP1_RDHB = 9.asUInt(4.W)
   val OP1_ZERO = 10.asUInt(4.W)

   val OP1_RS1 = 0.asUInt(4.W) // Register Source #1
   val OP1_IMU = 1.asUInt(4.W) // immediate, U-type
   val OP1_IMZ = 2.asUInt(4.W) // Zero-extended rs1 field of inst, for CSRI instructions
   val OP1_X   = 0.asUInt(4.W)

   // Operand 2 Select Signal
   val OP2_RDHD  = 4.asUInt(4.W)
   val OP2_IMMU6 = 5.asUInt(4.W)  // immu6 source in hd field
   val OP2_IMMS18 = 6.asUInt(4.W)  // imms18 source in hb/hc/hd field
   val OP2_IMMS12 = 7.asUInt(4.W)  // imms12 source in hc/hd field
   val OP2_IMMU12 = 8.asUInt(4.W)  // immu12 source in hc/hd field
   val OP2_RBHD = 9.asUInt(4.W)
   val OP2_WYDE = 10.asUInt(4.W)

   val OP2_RS2 = 0.asUInt(4.W) // Register Source #2
   val OP2_IMI = 1.asUInt(4.W) // immediate, I-type
   val OP2_IMS = 2.asUInt(4.W) // immediate, S-type
   val OP2_PC  = 3.asUInt(4.W) // PC
   val OP2_X   = 0.asUInt(4.W)

   // Register File Write Enable Signal
   val REN_0   = false.B
   val REN_1   = true.B
   val REN_X   = false.B

   // ALU Operation Signal
   val ALU_ADD = 1.asUInt(4.W)
   val ALU_SUB = 2.asUInt(4.W)
   val ALU_SLL = 3.asUInt(4.W)
   val ALU_SRL = 4.asUInt(4.W)
   val ALU_SRA = 5.asUInt(4.W)
   val ALU_AND = 6.asUInt(4.W)
   val ALU_OR  = 7.asUInt(4.W)
   val ALU_XOR = 8.asUInt(4.W)
   val ALU_CMPS = 9.asUInt(4.W)
   val ALU_CMPU= 10.asUInt(4.W)
   val ALU_COPY1= 11.asUInt(4.W)
   val ALU_COPY2= 12.asUInt(4.W)
   val ALU_SETOW= 13.asUInt(4.W)
   val ALU_ANDNW= 15.asUInt(4.W)
   val ALU_X   = 0.asUInt(4.W)

   // Writeback Select Signal
   val WB_RDHB  = 4.asUInt(4.W)
   val WB_RBHB  = 5.asUInt(4.W)
   val WB_RDHA  = 6.asUInt(4.W)
   val WB_RBHA  = 7.asUInt(4.W)
   val WB_HAHB  = 8.asUInt(4.W)   // two dest regs: rdha + rdhb
   val WB_RDMM  = 9.asUInt(4.W)
   val WB_RBMM  = 10.asUInt(4.W)

   val WB_ALU  = 0.asUInt(4.W)
   val WB_RA   = 2.asUInt(4.W)
   val WB_CSR  = 3.asUInt(4.W)
   val WB_X    = 0.asUInt(4.W)

   // Memory Function Type (Read,Write,Fence) Signal
   val MWR_R   = 0.asUInt(2.W)
   val MWR_W   = 1.asUInt(2.W)
   val MWR_F   = 2.asUInt(2.W)
   val MWR_X   = 0.asUInt(2.W)

   // Memory Enable Signal
   val MEN_0   = false.B
   val MEN_1   = true.B
   val MEN_X   = false.B

   // Memory Mask Type Signal
   val MSK_B   = 0.asUInt(3.W)
   val MSK_BU  = 1.asUInt(3.W)
   val MSK_H   = 2.asUInt(3.W)
   val MSK_HU  = 3.asUInt(3.W)
   val MSK_W   = 4.asUInt(3.W)
   val MSK_X   = 4.asUInt(3.W)


   // Cache Flushes & Sync Primitives
   val M_N      = 0.asUInt(3.W)
   val M_SI     = 1.asUInt(3.W)   // synch instruction stream
   val M_SD     = 2.asUInt(3.W)   // synch data stream
   val M_FA     = 3.asUInt(3.W)   // flush all caches
   val M_FD     = 4.asUInt(3.W)   // flush data cache

   // Memory Functions (read, write, fence)
   val MT_READ  = 0.asUInt(2.W)
   val MT_WRITE = 1.asUInt(2.W)
   val MT_FENCE = 2.asUInt(2.W)

}

}

