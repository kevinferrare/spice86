# Spice86 - PC emulator for 'real mode' reverse engineering
Reverse-engineering a program only from the binary is a complex task.
Based on a divide and conquer approach, Spice86 is able:
- Emulate your program
- Rewrite **real mode** DOS source code from execution
- Reverse-engineer the program, gradually re-implementing assembly code with your java methods

The benefits of this approach are:
  - Small sequences of assembly code favor static analyzis and high-level language translation
  - Always working on a fully working version of the program helps catching mistakes early
  - Rewriting the code function by function allows to discover the author's intents

***

# General

## Requirements

[java 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

[Maven 3.6+](https://maven.apache.org/docs/history.html)

## Limitations

#### _CPU:_

- Only 16-bits instructions are supported, memory size is 1MB
- The only supported addressing mode is real mode. 286/386 Protected mode and the related instructions are not implemented.
- Instruction set is (hopefully!) fully implemented for **8086** and validated via automated tests.
- For **80186**, BOUND, ENTER and LEAVE instructions are missing.
- For **80286**, instructions related to protected mode are not implemented
- For **80386**, protected mode and 32-bits instructions are not implemented. FS and GS registers are supported.
- No FPU instruction implemented except those used for FPU detection.
- GDB does not support x86 real mode segmented addressing, so pointers need to refer to the actual physical address in memory (VRAM address **A000:0000** would be **0xA0000** in GDB).
- The $pc variable in GDB will be exposed by Spice86 as the physical address pointed by CS:IP.

#### _Graphics:_

- Only VGA mode 0x13 is implemented

#### _DOS:_

- Part of int 21 is implemented. Identifies itself as DOS 5.0 for now.

#### _Input:_

- Keyboard
- Mouse (callback in mouse driver not implemented yet)
- Joystick not supported

#### _Sound:_

- No sound for now, games will not detect sound blaster or adlib.

Compatibility list availble [here](COMPATIBILITY.md).

## Lexicon

[Emulator](https://en.wikipedia.org/wiki/Emulator)

[Real mode](https://en.wikipedia.org/wiki/Real_mode)

[Reverse engineering](https://en.wikipedia.org/wiki/Reverse_engineering)

***

# Functional details

## Getting started with Spice86
Starting your program using Spice86 emulator is done with the regular **'java -jar'** command. 

Example with a program named **'file.exe'** (also supports **com** and **bios** files):
> java -jar spice86-1.27.0.jar file.exe

The option **'--gdbPort'** starts the GDB before Spice86 (allows to setup breakpoints and so on... read more later)

> java -jar spice86-1.27.0.jar file.exe --gdbPort=10000

## Dynamic analysis features

#### _Protocol + magic_
Spice86 uses the [GDB](https://www.gnu.org/software/gdb/) remote protocol:
- Supports most of the commands you need to debug (cf. Limitations section)
- Provides custom GDB commands for dynamic analysis (This is where the magic happens!)

#### _CLI commands_
Define GDB port:
> (gdb) target remote localhost:10000

Set architecture:
> (gdb) set architecture i8086

Breakpoint on VGA VRAM (pointers must refer to physical address as real mode segmented addressing is not supported)
> (gdb) watch *0xA0000

View assembly:
> (gdb) layout asm

Remove a breakpoint:
> (gdb) remove 1

Search sequence of bytes in memory (start address 0, length F0000, ascii bytes of 'Spice86' string):
> (gdb) find /b 0x0, 0xF0000, 0x53, 0x70, 0x69, 0x63, 0x65, 0x38, 0x36

## Custom commands detailed
Show the list of custom commands:
> (gdb) monitor help

#### _Dump memory to a file_
DOS programs can rewrite some of their instructions / load additional modules in memory.
Memory dumps can be useful to see curent assembly code being executed.
> (gdb) monitor dumpmemory path/to/dump.bin

#### _Dump functions to a file_
This will dump dynamic information about the encountered functions in the program:
> (gdb) monitor dumpfunctions path/to/functions.txt

For each function:
- Their address (both in segmented and physical addressing)
- Their name if override provided (read more about that later)
- The addresses of the returns that have been reached and their type (NEAR / FAR / INTERRUPT / MACHINE)
- The addresses of the returns that did not make the RET instruction point to the expected caller (some programs use RET as jump ...)
- The list of functions that calls it
- The list of functions it calls

Example:
```
function unknown_0x2538_0x151_0x254D1 returns:3 callers:1 called: 4 calls:3 approximateSize:11482
 - ret: FAR 0x2538:0x26AF/0x27A2F
 - ret: FAR 0x2538:0x2D41/0x280C1
 - ret: FAR 0x2538:0x2E2B/0x281AB
 - caller: unknown_0x1ED_0xC108_0xDFD8
 - call: vgaDriver.loadPalette_0x2538_0xB68_0x25EE8 overriden
 - call: vgaDriver.waitForRetraceInTransitions_0x2538_0x2572_0x278F2 overriden
 - call: unknown_0x2538_0x2596_0x27916
```

Here you can see that:
- The generated name unknown_0x2538_0x151_0x254D1 can be copy pasted directly in java to start overriding it.
- The physical address of the function is 0x254D1 in RAM (2538:0151 segmented)
- It spawns 11482 bytes (estimated distance between the entry point and the returns)
- Emulator encounterd several returns and it is called by one caller only
- It calls 3 other methods and 2 are overriden already

You can also dump the functions as CSV file (to import and processing in a spreadsheet):
> (gdb) monitor dumpfunctionscsv path/to/functions.txt

#### _Generate Java code_
Generate Java source code:
> (gdb) monitor dumpJavaStubs path/to/stub.java

This will generate java source code with:
- The function calls and how to override them
- Accessors for global variables (memory bytes accessed via hardcoded address)

#### _Special breakpoints_
Break after x emulated CPU Cycles:
> (gdb) monitor breakCycles 1000

Break at the end of the emulated program:
> (gdb) monitor breakStop

***

# Technical details

## Detailed reverse engineering process
Concrete example with Cryo Dune [here](https://github.com/kevinferrare/cryodunere).

1. Run your program and make sure everything works fine in Spice86. If you encounter issues it could be due to unimplemented hardware / DOS / BIOS features.
2. Run your program with the GDB server connected and set a breakpoint at the end of your program:

> (gdb) monitor breakStop
> 
> (gdb) continue


Run some interesting actions in your emulated program and quit it from the program. If you don't have this option, you can also break after a defined number of Cycles with **breakCycles**.

When GDB gives you control due to breakpoint being reached:
- Dump the memory with **monitor dumpmemory**
- Dump the functions with **monitor dumpfunctions**
- Dump the functions as CSV with **monitor dumpfunctionscsv**
- Dump the java stubs with **monitor dumpjavastubs**

Open the CSV file in a spreadsheet, filter functions that are overridable and not overriden:

![](doc/functions_csv.png)

Overridable means that the function calls no other function, or that it calls only overridden functions as calling Java from ASM is supported but ASM from Java is not.

If you sort by approximate size, you are likely to get the easiest targets first.

Note that approximate size does not always reflect the real size of the function as it is the difference between entry point address and encountered return address. A small function can sometimes be a monster that only got partially executed.

Also note that other values like callers can be wrong because sometimes the programs use returns to do jumps and this messes up the call stack analysis.

Open the memory dump in a disassembler / decompiler (I personally use [ghidra](https://github.com/NationalSecurityAgency/ghidra)).

In the screenshot, physical address of unknown_0x1ED_0xA1E8_0xC0B8 will be C0B8. The name contains both the segment:offset and physical addresses.

Go to this address:

![](doc/function_C0B8_ghidra.png)

As you can see, it is 2 lines and is very simple:
- Instruction at address C0B8 increases the byte at address DS:47A8 by one
- Instruction at address C0BC does a near ret

From there you can re-implement (override) the function and continue with the next one (see next chapter on how to do so).

Once you have a stub or the function implemented, you can put a java breakpoint in it to get a better understanding on how when the function is called and how it interacts with the rest of the code.

It is useful to document the relevant inputs and outputs.

To get a better understanding of the environment of a function, and especially what it calls, you can check it in the dump provided by **dumpfunctions**

## Overriding emulated code with Java code

This is where things start to get fun!

You can provide your own Java code to override the program original assembly code.

#### _Defining overrides_
Spice86 can take in input an instance of spice86.emulator.function.OverrideSupplier that builds a mapping between the memory address of functions and their java overrides.

For a complete example you can check the source code of [Cryo Dune RE](https://github.com/kevinferrare/cryodunere).

Here is a simple example of how it would look like:
```java
package my.program;
// This class is responsible for providing the overrides to spice86.
// There is only one per program you reimplement.
public class MyProgramOverrideSupplier implements OverrideSupplier {
  @Override
  public Map<SegmentedAddress, FunctionInformation> generateFunctionInformations(int programStartSegment,
      Machine machine) {
    Map<SegmentedAddress, FunctionInformation> res = new HashMap<>();
    // In more complex examples, overrides may call each other
    new MyOverrides(res, programStartSegment, machine);
    return res;
  }
  @Override
  public String toString() {
    return "Overrides My program exe. class is " + this.getClass().getCanonicalName();
  }
}
// This class contains the actual overrides. As the project grows, you will probably need to split the reverse engineered code in several classes.
public class MyOverrides extends JavaOverrideHelper {
  private MyOverridesGlobalsOnDs globalsOnDs;
  public MyOverrides(Map<SegmentedAddress, FunctionInformation> functionInformations, int segment, Machine machine) {
    // "myOverides" is a prefix that will be appended to all the function names defined in this class
    super(functionInformations, "myOverides", machine);
    globalsOnDs = new MyOverridesGlobalsOnDs(machine);
    // incUnknown47A8_0x1ED_0xA1E8_0xC0B8 will get executed instead of the assembly code when a call to 1ED:A1E8 is performed.
    // Also when dumping functions, the name myOverides.incUnknown47A8 or instead of unknown
    // Note: the segment is provided in parameter as spice86 can load executables in different places depending on the configuration
    defineFunction(segment, 0xA1E8, "incDialogueCount47A8", this::incDialogueCount47A8_0x1ED_0xA1E8_0xC0B8);
    defineFunction(segment, 0x0100, "addOneToAX", this::addOneToAX_0x1ED_0x100_0x1FD0);
  }
  public Runnable incDialogueCount47A8_0x1ED_0xA1E8_0xC0B8() {
    // Accessing the memory via accessors
    globalsOnDs.setDialogueCount47A8(globalsOnDs.getDialogueCount47A8() + 1);
    // Depends on the actual return instruction performed by the function, needed to be called from the emulated code as
    // some programs like to mess with the stack ...
    return nearRet();
  }
  private Runnable addOneToAX_0x1ED_0x100_0x1FD0() {
    // Assembly for this would be
    // INC AX
    // RETF
    // Note that you can access the whole emulator to change the state in the overrides.
    state.setAX(state.getAX() + 1);
    return nearRet();
  }
}
// Memory accesses can be encapsulated into classes like this to give names to addresses and make the code shorter.
public class MyOverridesGlobalsOnDs extends MemoryBasedDataStructureWithDsBaseAddress {
  public DialoguesGlobalsOnDs(Machine machine) {
    super(machine);
  }
  public void setDialogueCount47A8(int value) {
    this.setUint8(0x47A8, value);
  }
  public int getDialogueCount47A8() {
    return this.getUint8(0x47A8);
  }
}
```

To avoid mistakes, you can copy paste the java stubs generated by **monitor dumpjavastubs**

#### _Loading overrides_
Let's suppose that the overrides defined in the previous paragraph are in overrides.jar. Here is how you could launch Spice86 to use them:
```
java -cp 'overrides.jar:spice86-1.27.0.jar' spice86.main.Main file.exe --overrideSupplierClassName=my.program.MyProgramOverrideSupplier
```

If you just want to use the function names and not the overrides, you could add **--useCodeOverride=false** to the command line.

If you build a project around this, just call Spice86 like this in your main:
```java
  public static void main(String[] args) {
    Spice86Application.runWithOverrides(args, MyProgramOverrideSupplier.class);
  }
```
#### _Generating overrides_
The command dumpJavaStubs generates a text file with some java stubs that could be generated automatically.
```
(gdb) monitor dumpJavaStubs path/to/stubs.txt
```

Generated stub look like this:
```java
...
// defineFunction(0x2538, 0x151, "unknown", this::unknown_0x2538_0x151_0x254D1);
public Runnable unknown_0x2538_0x151_0x254D1() {
  return farRet();
}
...
```
You can copy paste the stub to your code.

## Misc

#### _C Drive_
It is possible to provide a C: Drive for emulated DOS functions with the option **--cDrive**. Default is current folder. For some games you may need to set the C drive to the game folder.

#### _Time_
The emulated Timer hardware of the PC (Intel 8259) supports measuring time from either:
- The real elapsed time. Speed can be altered with parameter **--timeMultiplier**.
- The number of instructions the emulated CPU executed. This behaviour is activated with the parameter **--instructionsPerSecond** and is forced when in GDB mode so that you can debug with peace of mine without the timer triggering.

#### _Screen refresh_
Screen is refreshed 30 times per second and each time a VGA retrace wait is detected (see VideoBiosServicesDispatcher::tick3DA).

***

# Some screenshots

#### _Cryo dune:_

![](doc/cryodune_worm.png)

![](doc/cryodune_orni.png)

#### _Prince of persia:_

![](doc/prince_of_persia.PNG)