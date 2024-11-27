import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util.MixedVec
import circt.stage.{ChiselStage, FirtoolOption}
import freechips.rocketchip.diplomacy.{AddressSet, IdRange, TransferSizes}
import org.chipsalliance.cde.config.{Config, Parameters}
import freechips.rocketchip.tilelink._
import org.chipsalliance.diplomacy.lazymodule._
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled

class Dut(implicit p: Parameters) extends LazyModule {
  private def slvParam(addressSet: AddressSet): TLSlavePortParameters = TLSlavePortParameters.v1(
    managers = Seq(TLSlaveParameters.v1(
      address = Seq(addressSet),
      supportsGet = TransferSizes(1, 8),
      supportsPutFull = TransferSizes(1, 8),
      supportsPutPartial = TransferSizes(1, 8),
    )),
    beatBytes = 4
  )
  private val mstParams = TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "master",
      sourceId = IdRange(0, 4)
    ))
  )
  private val mstNode = TLClientNode(Seq(mstParams))
  private val slvNode = TLManagerNode(Seq.tabulate(2)(i => slvParam(AddressSet(i * 0x1000L, (1 << 12) - 1))))
  private val xbar = LazyModule(new TLXbar)
  xbar.node :*= mstNode
  slvNode :*= xbar.node

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle {
      val mst = Flipped(new TLBundle(mstNode.out.head._1.params))
      val slv = MixedVec(slvNode.in.map(in => new TLBundle(in._1.params)))
    })
    io.mst <> mstNode.out.head._1
    for(i <- io.slv.indices) {
      io.slv(i) <> slvNode.in(i)._1
    }
    dontTouch(io)
  }
}

class Top(implicit p: Parameters) extends Module {
  private val dut = LazyModule(new Dut)
  private val _dut = Module(dut.module)
  private val timer = RegInit(0.U(32.W))
  timer := timer + 1.U

  private val slvDValid = Seq.fill(2)(RegInit(false.B))
  private val slvDOpcode = Seq.fill(2)(RegInit(0.U(3.W)))
  private val slvDSize = Seq.fill(2)(RegInit(0.U(2.W)))
  private val slvDSource = Seq.fill(2)(RegInit(0.U(2.W)))

  _dut.io.mst.a := DontCare
  _dut.io.mst.d.ready := true.B
  for(i <- _dut.io.slv.indices) {
    _dut.io.slv(i).a.ready := false.B
    _dut.io.slv(i).d.bits := DontCare
    _dut.io.slv(i).d.valid := slvDValid(i)
    _dut.io.slv(i).d.bits.opcode := slvDOpcode(i)
    _dut.io.slv(i).d.bits.size := slvDSize(i)
    _dut.io.slv(i).d.bits.source := slvDSource(i)
  }
  when(timer === 10.U) {
    slvDValid(0) := true.B
    slvDOpcode(0) := 1.U
    slvDSize(0) := 3.U
    slvDSource(0) := 0x0.U

    slvDValid(1) := true.B
    slvDOpcode(1) := 0.U
    slvDSize(1) := 3.U
    slvDSource(1) := 0x1.U
  }

  when(timer === 12.U) {
    slvDValid(1) := false.B
  }
  when(timer === 13.U) {
    slvDSource(0) := 0x2.U
  }
}

class MyConfig extends Config((site, here, up) => {
  case MonitorsEnabled => false
})

object DutMain extends App {
  private val firtoolOps = Seq(
    FirtoolOption("-O=release"),
    FirtoolOption("--disable-all-randomization"),
    FirtoolOption("--disable-annotation-unknown"),
    FirtoolOption("--strip-debug-info"),
    FirtoolOption("--lowering-options=noAlwaysComb," +
      " disallowPortDeclSharing, disallowLocalVariables," +
      " emittedLineLength=120, explicitBitcast," +
      " locationInfoStyle=plain, disallowMuxInlining")
  )
  (new ChiselStage).execute(args, firtoolOps ++ Seq(
    ChiselGeneratorAnnotation(() => new Top()(new MyConfig))
  ))
}