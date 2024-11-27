init:
	cd rocket-chip/dependencies && git submodule update --init cde hardfloat diplomacy

idea:
	mill -i mill.idea.GenIdea/idea

comp:
	mill -i tlxbar.compile

CWD = $(abspath .)

verilog: $(CWD)/src/main/scala/Top.scala
	
	mkdir -p build
	mill -i tlxbar.runMain DutMain --target systemverilog --full-stacktrace -td $(CWD)/build

VERILATOR_OPT = --exe --cc --top-module Top --x-assign unique --trace --threads 2
VERILATOR_OPT += -Mdir $(CWD)/sim/comp $(CWD)/build/Top.sv $(CWD)/src/test/cpp/main.cpp
VERILATOR_OPT += -CFLAGS "-std=c++17" -o $(CWD)/sim/run/emu

emu:
	mkdir -p $(CWD)/sim/comp
	mkdir -p $(CWD)/sim/run
	verilator $(VERILATOR_OPT)
	$(MAKE) -C $(CWD)/sim/comp -f VTop.mk -j8 VM_PARALLEL_BUILDS=1 OPT_FAST=-O3

sim:
	cd sim/run && ./emu

clean:
	rm -r build sim

.PHONY: clean sim