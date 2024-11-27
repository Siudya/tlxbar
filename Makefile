init:
	cd rocket-chip/dependencies && git submodule update --init cde hardfloat diplomacy

idea:
	mill -i mill.idea.GenIdea/idea

comp:
	mill -i tlxbar.compile

verilog:
	mkdir -p build
	mill -i tlxbar.runMain DutMain --target systemverilog --full-stacktrace -td build

VERILATOR_OPT = --exe --cc --top-module Top --x-assign unique --trace --threads 2
VERILATOR_OPT += -Mdir sim/emu build/Top.sv src/test/cpp/main.cpp
VERILATOR_OPT += -CFLAGS "-std=c++17" -o sim/run/emu

emu: verilog
	mkdir -p sim/comp
	mkdir -p sim/run
	verilator $(VERILATOR_OPT)
	$(MAKE) -C sim/comp -j8 VM_PARALLEL_BUILDS=1 OPT_FAST=-O3

sim: emu
	cd sim/run && ./emu

clean:
	rm -r build sim

.PHONY: clean