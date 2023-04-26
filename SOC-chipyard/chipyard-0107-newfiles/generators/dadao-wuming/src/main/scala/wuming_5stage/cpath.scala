//**************************************************************************
// RISCV Processor 5-Stage Control Path
//--------------------------------------------------------------------------
//
// Christopher Celio
// 2012 Jan 20
//
// Supports both a fully-bypassed datapath (with stalls for load-use), and a
// fully interlocked (no bypass) datapath that stalls for all hazards.

package wuming.stage5

import chisel3._
import chisel3.util._

import freechips.rocketchip.rocket.{CSR, Causes}

import Constants._
import wuming.common._
import wuming.common.Instructions._

class CtlToDatIo extends Bundle()
{
   val dec_stall  = Output(Bool())    // stall IF/DEC stages (due to hazards)
   val full_stall = Output(Bool())    // stall entire pipeline (due to D$ misses)
   val exe_pc_sel = Output(UInt(2.W))
   val br_type    = Output(UInt(4.W))
   val cf_type    = Output(UInt(3.W))
   val cd_type    = Output(UInt(4.W))
   val if_kill    = Output(Bool())
   val dec_kill   = Output(Bool())
   val op1_sel    = Output(UInt(2.W))
   val op2_sel    = Output(UInt(3.W))
   val alu_fun    = Output(UInt(4.W))
   val wb_sel     = Output(UInt(2.W))
   val rf_wen     = Output(Bool())
   val mem_val    = Output(Bool())
   val mem_fcn    = Output(UInt(2.W))
   val mem_typ    = Output(UInt(3.W))
   val csr_cmd    = Output(UInt(CSR.SZ.W))
   val fencei     = Output(Bool())    // pipeline is executing a fencei

   val pipeline_kill = Output(Bool()) // an exception occurred (detected in mem stage).
                                    // Kill the entire pipeline disregard stalls
                                    // and kill if,dec,exe stages.
   val mem_exception = Output(Bool()) // tell the CSR that the core detected an exception
   val mem_exception_cause = Output(UInt(32.W))
   val dec_reg_grp   = Output(UInt(REG_X.getWidth.W))
   
   val cnd_fun   = Output(UInt(COND_X.getWidth.W))
}

class CpathIo(implicit val conf: WumingCoreParams) extends Bundle()
{
   val dcpath = Flipped(new DebugCPath())
   val imem = new MemPortIo(conf.instlen)
   val dmem = new MemPortIo(conf.xprlen)
   val dat  = Flipped(new DatToCtlIo())
   val ctl  = new CtlToDatIo()
}


class CtlPath(implicit val conf: WumingCoreParams) extends Module
{
  val io = IO(new CpathIo())
  io := DontCare

   val csignals =
      ListLookup(io.dat.dec_inst,
                       List(N, CF_X    , COND_X, REG_X    , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X, CSR.N, N),
         Array(      /* val  | Control | cond  | reg      | op1       |    op2      |    ALU     | wb       | mem  | mem  | mask | csr | fence*/
                     /* inst |   flow  |  fcn  |  set     |  sel      |    sel      |    fcn     | sel      |  en  |  wr  | type | cmd | */
            SWYM    -> List(Y, CF_X    , COND_X, REG_X    , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X, CSR.N, N),
            FENCE   -> List(Y, CF_X    , COND_X, REG_X    , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X, CSR.N, Y),
            RD2RD   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_X   , S_OP2_RDHC  , S_ALU_COPY2, S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            RD2RB   -> List(Y, CF_X    , COND_X, REG_MRB  , S_OP1_X   , S_OP2_RDHC  , S_ALU_COPY2, S_WB_RBHB, MEN_0, M_X  , MT_X, CSR.N, N),
            RB2RD   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_X   , S_OP2_RBHC  , S_ALU_COPY2, S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            RB2RB   -> List(Y, CF_X    , COND_X, REG_MRB  , S_OP1_X   , S_OP2_RBHC  , S_ALU_COPY2, S_WB_RBHB, MEN_0, M_X  , MT_X, CSR.N, N),
            RD2RF   -> List(Y, CF_X    , COND_X, REG_MRF  , S_OP1_X   , S_OP2_RDHC  , S_ALU_COPY2, S_WB_RFHB, MEN_0, M_X  , MT_X, CSR.N, N),
            RF2RD   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_X   , S_OP2_RFHC  , S_ALU_COPY2, S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            RF2RF   -> List(Y, CF_X    , COND_X, REG_MRF  , S_OP1_X   , S_OP2_RFHC  , S_ALU_COPY2, S_WB_RFHB, MEN_0, M_X  , MT_X, CSR.N, N),

            AND     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_AND  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            ORR     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_OR   , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            XOR     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_XOR  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            XNOR    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_XNOR , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),

            ANDI    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHB, S_OP2_IMMU12, S_ALU_AND  , S_WB_RDHA, MEN_0, M_X  , MT_X, CSR.N, N),

            SHLUr   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_SLL  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SHLUi   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_IMMU6 , S_ALU_SLL  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SHRSr   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_SRA  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SHRSi   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_IMMU6 , S_ALU_SRA  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SHRUr   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_SRL  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SHRUi   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_IMMU6 , S_ALU_SRL  , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            EXTSr   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_EXTS , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            EXTSi   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_IMMU6 , S_ALU_EXTS , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            EXTZr   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_EXTZ , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),
            EXTZi   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_IMMU6 , S_ALU_EXTZ , S_WB_RDHB, MEN_0, M_X  , MT_X, CSR.N, N),

            ADDrb   -> List(Y, CF_X    , COND_X, REG_RB   , S_OP1_RBHC, S_OP2_RDHD  , S_ALU_ADD  , S_WB_RBHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SUBrb   -> List(Y, CF_X    , COND_X, REG_RB   , S_OP1_RBHC, S_OP2_RDHD  , S_ALU_SUB  , S_WB_RBHB, MEN_0, M_X  , MT_X, CSR.N, N),

            ADDIrd  -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDHA, MEN_0, M_X  , MT_X, CSR.N, N),
            ADDIrb  -> List(Y, CF_X    , COND_X, REG_RB   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RBHA, MEN_0, M_X  , MT_X, CSR.N, N),

            ADDrd   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_ADD  , S_WB_HAHB, MEN_0, M_X  , MT_X, CSR.N, N),
            SUBrd   -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_SUB  , S_WB_HAHB, MEN_0, M_X  , MT_X, CSR.N, N),

            MULS    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_MULS , S_WB_HAHB, MEN_0, M_X  , MT_X, CSR.N, N),
            MULU    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_MULU , S_WB_HAHB, MEN_0, M_X  , MT_X, CSR.N, N),
            DIVS    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_DIVS , S_WB_HAHB, MEN_0, M_X  , MT_X, CSR.N, N),
            DIVU    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_DIVU , S_WB_HAHB, MEN_0, M_X  , MT_X, CSR.N, N),

            LDBS    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_B , CSR.N, N),
            LDWS    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_W , CSR.N, N),
            LDTS    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_T , CSR.N, N),
            LDO     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_O , CSR.N, N),
            LDBU    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_BU, CSR.N, N),
            LDWU    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_WU, CSR.N, N),
            LDTU    -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_TU, CSR.N, N),

            LDMBS   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_B , CSR.N, N),
            LDMWS   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_W , CSR.N, N),
            LDMTS   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_T , CSR.N, N),
            LDMO    -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_O , CSR.N, N),
            LDMBU   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_BU, CSR.N, N),
            LDMWU   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_WU, CSR.N, N),
            LDMTU   -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RDMM, MEN_1, M_XRD, MT_TU, CSR.N, N),

            STB     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_B , CSR.N, N),
            STW     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_W , CSR.N, N),
            STT     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_T , CSR.N, N),
            STO     -> List(Y, CF_X    , COND_X, REG_RD   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_O , CSR.N, N),

            STMB    -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_B , CSR.N, N),
            STMW    -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_W , CSR.N, N),
            STMT    -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_T , CSR.N, N),
            STMO    -> List(Y, CF_X    , COND_X, REG_MRD  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_O , CSR.N, N),

            LDRB    -> List(Y, CF_X    , COND_X, REG_RB   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_RBMM, MEN_1, M_XRD, MT_O , CSR.N, N),
            STRB    -> List(Y, CF_X    , COND_X, REG_RB   , S_OP1_RBHB, S_OP2_IMMS12, S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_O , CSR.N, N),

            LDMRB   -> List(Y, CF_X    , COND_X, REG_MRB  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_RBMM, MEN_1, M_XRD, MT_O , CSR.N, N),
            STMRB   -> List(Y, CF_X    , COND_X, REG_MRB  , S_OP1_RBHB, S_OP2_RDHC  , S_ALU_ADD  , S_WB_X   , MEN_1, M_XWR, MT_O , CSR.N, N),

            CMPSi   -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_RDHB, S_OP2_IMMS12, S_ALU_CMPS , S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),
            CMPSr   -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_CMPS , S_WB_RDHB, MEN_0, M_X  , MT_X , CSR.N, N),
            CMPUi   -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_RDHB, S_OP2_IMMU12, S_ALU_CMPU , S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),
            CMPUr   -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_RDHC, S_OP2_RDHD  , S_ALU_CMPU , S_WB_RDHB, MEN_0, M_X  , MT_X , CSR.N, N),
            CMP     -> List(Y, CF_X    , COND_X , REG_RB  , S_OP1_RBHC, S_OP2_RBHD  , S_ALU_CMPU , S_WB_RDHB, MEN_0, M_X  , MT_X , CSR.N, N),

            BREQ    -> List(Y, CF_BR12 , COND_EQ, REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRNE    -> List(Y, CF_BR12 , COND_NE, REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRN     -> List(Y, CF_BR18 , COND_N , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRNN    -> List(Y, CF_BR18 , COND_NN, REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRZ     -> List(Y, CF_BR18 , COND_Z , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRNZ    -> List(Y, CF_BR18 , COND_NZ, REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRP     -> List(Y, CF_BR18 , COND_P , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            BRNP    -> List(Y, CF_BR18 , COND_NP, REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),

            JUMPi   -> List(Y, CF_JUMPI, COND_X , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            JUMPr   -> List(Y, CF_JUMPR, COND_X , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            CALLi   -> List(Y, CF_CALLI, COND_X , RAS_PUSH, S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_RA  , MEN_0, M_X  , MT_X , CSR.N, N),
            CALLr   -> List(Y, CF_CALLR, COND_X , RAS_PUSH, S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_RA  , MEN_0, M_X  , MT_X , CSR.N, N),
            RET     -> List(Y, CF_RET  , COND_X , RAS_POP , S_OP1_X   , S_OP2_IMMS18, S_ALU_COPY2, S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),

            SETZWrd -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_X   , S_OP2_WYDE  , S_ALU_COPY2, S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),
            SETZWrb -> List(Y, CF_X    , COND_X , REG_RB  , S_OP1_X   , S_OP2_WYDE  , S_ALU_COPY2, S_WB_RBHA, MEN_0, M_X  , MT_X , CSR.N, N),
            SETOW   -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_X   , S_OP2_WYDE  , S_ALU_SETOW, S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),
            ORWrd   -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_RDHA, S_OP2_WYDE  , S_ALU_OR   , S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),
            ORWrb   -> List(Y, CF_X    , COND_X , REG_RB  , S_OP1_RBHA, S_OP2_WYDE  , S_ALU_OR   , S_WB_RBHA, MEN_0, M_X  , MT_X , CSR.N, N),
            ANDNWrd -> List(Y, CF_X    , COND_X , REG_RD  , S_OP1_RDHA, S_OP2_WYDE  , S_ALU_ANDNW, S_WB_RDHA, MEN_0, M_X  , MT_X , CSR.N, N),
            ANDNWrb -> List(Y, CF_X    , COND_X , REG_RB  , S_OP1_RBHA, S_OP2_WYDE  , S_ALU_ANDNW, S_WB_RBHA, MEN_0, M_X  , MT_X , CSR.N, N),
            SETW    -> List(Y, CF_X    , COND_X , REG_RF  , S_OP1_RFHA, S_OP2_WYDE  , S_ALU_SETW , S_WB_RFHA, MEN_0, M_X  , MT_X , CSR.N, N),

            ADRP    -> List(Y, CF_X    , COND_X , REG_X   , S_OP1_PC  , S_OP2_IMMS18, S_ALU_ADRP , S_WB_RBHA, MEN_0, M_X  , MT_X , CSR.N, N),

            CPCO    -> List(Y, CF_X    , COND_X , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.N, N),
            CPRD    -> List(Y, CF_X    , COND_X , REG_CSR , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_CSR , MEN_0, M_X  , MT_X , CSR.R, N),
            CPWR    -> List(Y, CF_X    , COND_X , REG_CSR , S_OP1_X   , S_OP2_RDHD  , S_ALU_COPY2, S_WB_X   , MEN_0, M_X  , MT_X , CSR.W, N),
            TRAP    -> List(Y, CF_X    , COND_X , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.I, N),
            ESCAPE  -> List(Y, CF_X    , COND_X , REG_X   , S_OP1_X   , S_OP2_X     , S_ALU_X    , S_WB_X   , MEN_0, M_X  , MT_X , CSR.I, N),
               ))

   // Put these control signals in variables
   // val (cs_val_inst: Bool) :: cs_br_type :: cs_op1_sel :: cs_op2_sel :: (cs_rs1_oen: Bool) :: (cs_rs2_oen: Bool) :: cs0 = csignals
   // val cs_alu_fun :: cs_wb_sel :: (cs_rf_wen: Bool) :: (cs_mem_en: Bool) :: cs_mem_fcn :: cs_msk_sel :: cs_csr_cmd :: (cs_fencei: Bool) :: Nil = cs0

   // val cs_rs1_oen = N
   // val cs_rs2_oen = N
   
   val (cs_val_inst: Bool) :: cs_ctrl_flow  :: cs_cond_fun       :: cs_reg_group  :: cs_op1_sel :: cs_op2_sel :: cs0 = csignals
   val cs_alu_fun          :: cs_wb_sel     :: /*(cs_rf_wen: Bool) ::   */            cs1 = cs0
   val (cs_mem_en: Bool)   :: cs_mem_fcn    :: cs_msk_sel        :: cs_csr_cmd    :: (cs_fencei: Bool) :: Nil = cs1
//  S_PC_4     
//  S_PC_EXCP  
//  S_PC_BR12  
//  S_PC_BR18  
//  S_PC_IIII  
//  S_PC_RRII  
//  S_PC_RASP  

// CF_X    
// CF_BR12 
// CF_BR18 
// CF_JUMPI
// CF_JUMPR
// CF_CALLI
// CF_CALLR
// CF_RET  
//    // Branch Logic
   val ctrl_exe_pc_sel = MuxCase ( S_PC_4, Array(
      io.ctl.pipeline_kill              -> S_PC_EXCP,
      (io.dat.exe_cf_type === CF_X)     -> S_PC_4,
      (io.dat.exe_cf_type === CF_BR12)  -> Mux(io.dat.exe_cd_type === COND_EQ , Mux( io.dat.exe_cond_eq, S_PC_BR12, S_PC_4),
                                           Mux(io.dat.exe_cd_type === COND_NE , Mux(!io.dat.exe_cond_eq, S_PC_BR12, S_PC_4), S_PC_4)),
      (io.dat.exe_cf_type === CF_BR18)  -> Mux(io.dat.exe_cd_type === COND_Z  , Mux( io.dat.exe_cond_z, S_PC_BR18, S_PC_4),
                                           Mux(io.dat.exe_cd_type === COND_NZ , Mux(!io.dat.exe_cond_z, S_PC_BR18, S_PC_4),
                                           Mux(io.dat.exe_cd_type === COND_P  , Mux( io.dat.exe_cond_p, S_PC_BR18, S_PC_4),
                                           Mux(io.dat.exe_cd_type === COND_NP , Mux(!io.dat.exe_cond_p, S_PC_BR18, S_PC_4),
                                           Mux(io.dat.exe_cd_type === COND_N  , Mux( io.dat.exe_cond_n, S_PC_BR18, S_PC_4),
                                           Mux(io.dat.exe_cd_type === COND_NN , Mux(!io.dat.exe_cond_n, S_PC_BR18, S_PC_4), S_PC_4)))))),
      (io.dat.exe_cf_type === CF_JUMPI) -> S_PC_IIII,
      (io.dat.exe_cf_type === CF_JUMPR) -> S_PC_RRII,
      (io.dat.exe_cf_type === CF_CALLI) -> S_PC_IIII,
      (io.dat.exe_cf_type === CF_CALLR) -> S_PC_RRII,
      (io.dat.exe_cf_type === CF_RET)   -> S_PC_RASP,
      
   ))
   // Mux(io.ctl.pipeline_kill         , PC_EXC,
   //                       Mux(io.dat.exe_br_type === BR_N  , PC_4,
   //                       Mux(io.dat.exe_br_type === BR_NE , Mux(!io.dat.exe_br_eq,  PC_BRJMP, PC_4),
   //                       Mux(io.dat.exe_br_type === BR_EQ , Mux( io.dat.exe_br_eq,  PC_BRJMP, PC_4),
   //                       Mux(io.dat.exe_br_type === BR_GE , Mux(!io.dat.exe_br_lt,  PC_BRJMP, PC_4),
   //                       Mux(io.dat.exe_br_type === BR_GEU, Mux(!io.dat.exe_br_ltu, PC_BRJMP, PC_4),
   //                       Mux(io.dat.exe_br_type === BR_LT , Mux( io.dat.exe_br_lt,  PC_BRJMP, PC_4),
   //                       Mux(io.dat.exe_br_type === BR_LTU, Mux( io.dat.exe_br_ltu, PC_BRJMP, PC_4),
   //                       Mux(io.dat.exe_br_type === BR_J  , PC_BRJMP,
   //                       Mux(io.dat.exe_br_type === BR_JR , PC_JALR,
   //                                                          PC_4
   //                   ))))))))))

   val ifkill  = (ctrl_exe_pc_sel =/= PC_4) || cs_fencei || RegNext(cs_fencei)
   val deckill = (ctrl_exe_pc_sel =/= PC_4)

   // Exception Handling ---------------------

   io.ctl.pipeline_kill := (io.dat.csr_eret || io.ctl.mem_exception || io.dat.csr_interrupt)

   val dec_illegal = (!cs_val_inst && io.dat.dec_valid)

   // Stall Signal Logic --------------------
   val stall   = Wire(Bool())

   val dec_ha_addr  = io.dat.dec_inst(HA_MSB, HA_LSB)
   val dec_hb_addr  = io.dat.dec_inst(HB_MSB, HB_LSB)
   val dec_hc_addr  = io.dat.dec_inst(HC_MSB, HC_LSB)
   val dec_hd_addr  = io.dat.dec_inst(HD_MSB, HD_LSB)
   
   val dec_wb_addr  = MuxCase(dec_ha_addr, Array(
                        (cs_wb_sel === S_WB_RDHB) -> dec_hb_addr,
                        (cs_wb_sel === S_WB_RDHC) -> dec_hc_addr,
                        (cs_wb_sel === S_WB_RBHB) -> dec_hb_addr,
                        (cs_wb_sel === S_WB_RFHB) -> dec_hb_addr,
                        (cs_wb_sel === S_WB_HAHB) -> dec_hb_addr,
                        (cs_wb_sel === S_WB_CSR)  -> dec_hd_addr,
                     ))
   val dec_wb2_addr = dec_ha_addr
   
   val dec_wb_rf    = Reg(UInt(3.W))
   val dec_wb2_rf   = Reg(UInt(3.W))

   dec_wb_rf       := MuxCase(RFX2, Array(
                        (cs_wb_sel === S_WB_RDHA) -> RFD,
                        (cs_wb_sel === S_WB_RDHB) -> RFD,
                        (cs_wb_sel === S_WB_RDHC) -> RFD,
                        (cs_wb_sel === S_WB_RBHA) -> RFB,
                        (cs_wb_sel === S_WB_RBHB) -> RFB,
                        (cs_wb_sel === S_WB_RFHA) -> RFF,
                        (cs_wb_sel === S_WB_RFHB) -> RFF,
                        (cs_wb_sel === S_WB_HAHB) -> RFD,
                        (cs_wb_sel === S_WB_CSR)  -> RFD,
                        (cs_wb_sel === S_WB_RA)   -> RFA
                     ))
   dec_wb2_rf      := Mux((cs_wb_sel === S_WB_HAHB), RFD, RFX2)

   val dec_op1_addr = MuxCase(dec_ha_addr, Array(
                        (cs_op1_sel === S_OP1_RDHA) -> dec_ha_addr,
                        (cs_op1_sel === S_OP1_RDHB) -> dec_hb_addr,
                        (cs_op1_sel === S_OP1_RDHC) -> dec_hc_addr,
                        (cs_op1_sel === S_OP1_RBHA) -> dec_ha_addr,
                        (cs_op1_sel === S_OP1_RBHB) -> dec_hb_addr,
                        (cs_op1_sel === S_OP1_RBHC) -> dec_hc_addr,
                        (cs_op1_sel === S_OP1_RFHA) -> dec_ha_addr,
                     ))
   val dec_op1_rf   = MuxCase(RFX, Array(
                        (cs_op1_sel === S_OP1_RDHA) -> RFD,
                        (cs_op1_sel === S_OP1_RDHB) -> RFD,
                        (cs_op1_sel === S_OP1_RDHC) -> RFD,
                        (cs_op1_sel === S_OP1_RBHA) -> RFB,
                        (cs_op1_sel === S_OP1_RBHB) -> RFB,
                        (cs_op1_sel === S_OP1_RBHC) -> RFB,
                        (cs_op1_sel === S_OP1_RFHA) -> RFF,
                     ))

   val dec_op2_addr = MuxCase(dec_ha_addr, Array(
                        (cs_op2_sel === S_OP2_RDHC) -> dec_hc_addr,
                        (cs_op2_sel === S_OP2_RDHD) -> dec_hd_addr,
                        (cs_op2_sel === S_OP2_RBHC) -> dec_hc_addr,
                        (cs_op2_sel === S_OP2_RBHD) -> dec_hd_addr,
                        (cs_op2_sel === S_OP2_RFHC) -> dec_hc_addr,
                     ))
   val dec_op2_rf   = MuxCase(RFX, Array(
                        (cs_op2_sel === S_OP2_RDHC) -> RFD,
                        (cs_op2_sel === S_OP2_RDHD) -> RFD,
                        (cs_op2_sel === S_OP2_RBHC) -> RFB,
                        (cs_op2_sel === S_OP2_RBHD) -> RFB,
                        (cs_op2_sel === S_OP2_RFHC) -> RFF,
                     ))

   val dec_cond1_rf = MuxCase(RFX, Array(
                        (cs_cond_fun === COND_EQ) -> RFD,
                        (cs_cond_fun === COND_NE) -> RFD,
                        (cs_cond_fun === COND_Z)  -> RFD,
                        (cs_cond_fun === COND_NZ) -> RFD,
                        (cs_cond_fun === COND_P)  -> RFD,
                        (cs_cond_fun === COND_NP) -> RFD,
                        (cs_cond_fun === COND_N)  -> RFD,
                        (cs_cond_fun === COND_NN) -> RFD,
                     ))
                     
   val dec_cond2_rf = MuxCase(RFX, Array(
                        (cs_cond_fun === COND_EQ) -> RFD,
                        (cs_cond_fun === COND_NE) -> RFD,
                     ))

   // val dec_rs1_addr = io.dat.dec_inst(19, 15)
   // val dec_rs2_addr = io.dat.dec_inst(24, 20)
   // val dec_wbaddr   = io.dat.dec_inst(11, 7)
   // val dec_rs1_oen  = Mux(deckill, false.B, cs_rs1_oen)
   // val dec_rs2_oen  = Mux(deckill, false.B, cs_rs2_oen)

   val exe_reg_wbaddr      = Reg(UInt())
   val mem_reg_wbaddr      = Reg(UInt())
   val wb_reg_wbaddr       = Reg(UInt())
   val exe_reg_wbrf        = Reg(UInt())
   val mem_reg_wbrf        = Reg(UInt())
   val wb_reg_wbrf         = Reg(UInt())
   val exe_reg_wb2addr     = Reg(UInt())
   val mem_reg_wb2addr     = Reg(UInt())
   val wb_reg_wb2addr      = Reg(UInt())
   val exe_reg_wb2rf       = Reg(UInt())
   val mem_reg_wb2rf       = Reg(UInt())
   val wb_reg_wb2rf        = Reg(UInt())
   // val exe_reg_ctrl_rf_wen = RegInit(false.B)
   // val mem_reg_ctrl_rf_wen = RegInit(false.B)
   // val wb_reg_ctrl_rf_wen  = RegInit(false.B)
   val exe_reg_illegal     = RegInit(false.B)

   val exe_reg_is_csr = RegInit(false.B)

   // TODO rename stall==hazard_stall full_stall == cmiss_stall
   val full_stall = Wire(Bool())
   when (!stall && !full_stall)
   {
      when (deckill)
      {
         exe_reg_wbaddr      := 0.U
         exe_reg_wb2addr     := 0.U
         exe_reg_wbrf        := RFX2
         exe_reg_wb2rf       := RFX2
         // exe_reg_ctrl_rf_wen := false.B
         exe_reg_is_csr      := false.B
         exe_reg_illegal     := false.B
      }
      .otherwise
      {
         exe_reg_wbaddr      := dec_wb_addr
         exe_reg_wbrf        := dec_wb_rf
         exe_reg_wb2addr      := dec_wb2_addr
         exe_reg_wb2rf        := dec_wb2_rf
         // exe_reg_ctrl_rf_wen := cs_rf_wen
         exe_reg_is_csr      := cs_csr_cmd =/= CSR.N && cs_csr_cmd =/= CSR.I
         exe_reg_illegal     := dec_illegal
      }
   }
   .elsewhen (stall && !full_stall)
   {
      // kill exe stage
      exe_reg_wbaddr      := 0.U
      exe_reg_wb2addr     := 0.U
      exe_reg_wbrf        := RFX2
      exe_reg_wb2rf       := RFX2
      // exe_reg_ctrl_rf_wen := false.B
      exe_reg_is_csr      := false.B
      exe_reg_illegal     := false.B
   }
   when (!full_stall) {
     mem_reg_wbaddr      := exe_reg_wbaddr
     mem_reg_wbrf        := exe_reg_wbrf
     mem_reg_wb2addr     := exe_reg_wb2addr
     mem_reg_wb2rf       := exe_reg_wb2rf
     wb_reg_wbaddr       := mem_reg_wbaddr
     wb_reg_wbrf         := mem_reg_wbrf
     wb_reg_wb2addr      := mem_reg_wb2addr
     wb_reg_wb2rf        := mem_reg_wb2rf
   //   mem_reg_ctrl_rf_wen := exe_reg_ctrl_rf_wen
   //   wb_reg_ctrl_rf_wen  := mem_reg_ctrl_rf_wen
   }

   val exe_inst_is_load = RegInit(false.B)

   when (!full_stall)
   {
      exe_inst_is_load := cs_mem_en && (cs_mem_fcn === M_XRD)
   }

   // Clear instruction exception (from the "instruction" following xret) when returning from trap
   when (io.dat.csr_eret)
   {
      exe_reg_illegal    := false.B
   }

   // Stall signal stalls instruction fetch & decode stages,
   // inserts NOP into execute stage,  and drains execute, memory, and writeback stages
   // stalls on I$ misses and on hazards
   if (USE_FULL_BYPASSING)
   {
      // stall for load-use hazard
      stall := ((exe_inst_is_load) && (exe_reg_wbaddr === dec_op1_addr) && (exe_reg_wbrf === dec_op1_rf)) ||
               ((exe_inst_is_load) && (exe_reg_wbaddr === dec_op2_addr) && (exe_reg_wbrf === dec_op2_rf)) ||
               ((exe_inst_is_load) && (exe_reg_wbaddr === dec_ha_addr) && (exe_reg_wbrf === dec_cond1_rf)) ||
               ((exe_inst_is_load) && (exe_reg_wbaddr === dec_hb_addr) && (exe_reg_wbrf === dec_cond2_rf)) ||
               (exe_reg_is_csr)
   }
   else
   {
      // stall for all hazards
      stall := ((exe_reg_wbaddr === dec_op1_addr) && (exe_reg_wbrf === dec_op1_rf)) ||
               ((mem_reg_wbaddr === dec_op1_addr) && (mem_reg_wbrf === dec_op1_rf)) ||
               ((wb_reg_wbaddr  === dec_op1_addr) && (wb_reg_wbrf  === dec_op1_rf)) ||
               ((exe_reg_wbaddr === dec_op2_addr) && (exe_reg_wbrf === dec_op2_rf)) ||
               ((mem_reg_wbaddr === dec_op2_addr) && (mem_reg_wbrf === dec_op2_rf)) ||
               ((wb_reg_wbaddr  === dec_op2_addr) && (wb_reg_wbrf  === dec_op2_rf)) ||
               ((exe_reg_wb2addr === dec_op1_addr) && (exe_reg_wb2rf === dec_op1_rf)) ||
               ((mem_reg_wb2addr === dec_op1_addr) && (mem_reg_wb2rf === dec_op1_rf)) ||
               ((wb_reg_wb2addr  === dec_op1_addr) && (wb_reg_wb2rf  === dec_op1_rf)) ||
               ((exe_reg_wb2addr === dec_op2_addr) && (exe_reg_wb2rf === dec_op2_rf)) ||
               ((mem_reg_wb2addr === dec_op2_addr) && (mem_reg_wb2rf === dec_op2_rf)) ||
               ((wb_reg_wb2addr  === dec_op2_addr) && (wb_reg_wb2rf  === dec_op2_rf)) ||
               ((exe_reg_wbaddr === dec_ha_addr) && (exe_reg_wbrf === dec_cond1_rf)) ||
               ((mem_reg_wbaddr === dec_ha_addr) && (mem_reg_wbrf === dec_cond1_rf)) ||
               ((wb_reg_wbaddr  === dec_ha_addr) && (wb_reg_wbrf  === dec_cond1_rf)) ||
               ((exe_reg_wbaddr === dec_hb_addr) && (exe_reg_wbrf === dec_cond2_rf)) ||
               ((mem_reg_wbaddr === dec_hb_addr) && (mem_reg_wbrf === dec_cond2_rf)) ||
               ((wb_reg_wbaddr  === dec_hb_addr) && (wb_reg_wbrf  === dec_cond2_rf)) ||
               // ((exe_inst_is_load) && (exe_reg_wbaddr === dec_op1_addr) && (exe_reg_wbrf === dec_op1_rf)) ||
               // ((exe_inst_is_load) && (exe_reg_wbaddr === dec_op2_addr) && (exe_reg_wbrf === dec_op2_rf)) ||
               ((exe_reg_is_csr))
   }


   // stall full pipeline on D$ miss
   val dmem_val   = io.dat.mem_ctrl_dmem_val
   full_stall := !((dmem_val && io.dmem.resp.valid) || !dmem_val)


   io.ctl.dec_stall  := stall // stall if, dec stage (pipeline hazard)
   io.ctl.full_stall := full_stall // stall entire pipeline (cache miss)
   io.ctl.exe_pc_sel := ctrl_exe_pc_sel
   io.ctl.cf_type    := cs_ctrl_flow
   io.ctl.cd_type    := cs_cond_fun
   io.ctl.if_kill    := ifkill
   io.ctl.dec_kill   := deckill
   io.ctl.op1_sel    := cs_op1_sel
   io.ctl.op2_sel    := cs_op2_sel
   io.ctl.alu_fun    := cs_alu_fun
   io.ctl.wb_sel     := cs_wb_sel
   // io.ctl.rf_wen     := cs_rf_wen

   // we need to stall IF while fencei goes through DEC and EXE, as there may
   // be a store we need to wait to clear in MEM.
   io.ctl.fencei     := cs_fencei || RegNext(cs_fencei)

   // Exception priority matters!
   io.ctl.mem_exception := RegNext((exe_reg_illegal || io.dat.exe_inst_misaligned) && !io.dat.csr_eret) || io.dat.mem_data_misaligned
   io.ctl.mem_exception_cause := Mux(RegNext(exe_reg_illegal),            Causes.illegal_instruction.U,
                                 Mux(RegNext(io.dat.exe_inst_misaligned), Causes.misaligned_fetch.U,
                                 Mux(io.dat.mem_store,                    Causes.misaligned_store.U,
                                                                          Causes.misaligned_load.U
                                 )))

   // convert CSR instructions with raddr1 == 0 to read-only CSR commands
   val rs1_addr = io.dat.dec_inst(RS1_MSB, RS1_LSB)
   io.ctl.csr_cmd := cs_csr_cmd

   io.ctl.mem_val    := cs_mem_en
   io.ctl.mem_fcn    := cs_mem_fcn
   io.ctl.mem_typ    := cs_msk_sel

}
