#include "VTop.h"
#include <verilated_vcd_c.h>
#include <cstdint>
using namespace std;

void step(VTop *dut_ptr, VerilatedVcdC *tfp) {
  static uint64_t timer = 0;
  dut_ptr->clock = 1;
  dut_ptr->eval();
  tfp->dump(2 * timer);
  dut_ptr->clock = 0;
  dut_ptr->eval();
  tfp->dump(2 * timer + 1);
  ++timer;
}

int main() {
  VTop *dut_ptr = new VTop;
  Verilated::traceEverOn(true);
  VerilatedVcdC *tfp = new VerilatedVcdC;
  dut_ptr->trace(tfp, 0);
  tfp->open("sim.vcd");
  uint64_t timer = 0;
  dut_ptr->clock = 0;
  dut_ptr->reset = 1;
  for(int i = 0; i < 10; i++) step(dut_ptr, tfp);
  dut_ptr->reset = 0;
  for(int i = 0; i < 90; i++) step(dut_ptr, tfp);
  tfp->close();
  dut_ptr->final();
}