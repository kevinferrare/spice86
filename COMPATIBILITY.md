A lot of programs are not running.

This is mainly because:
 - The only currently supported display mode is VGA mode 13h
    - Text mode is not supported (for option input at the beginning of the game for example).
    - CGA / EGA games are not supported
 - TSR / sub programs / Extended memory is not implemented

Here is a list of old games I tested with what worked and what didn't:

| Program | State | Comment | Update date |
|--|--|--|--|
| Alley Cat | :see_no_evil: Crashes | CGA not implemented | 2021/09/26 |
| Alone in the dark | :see_no_evil: Crashes | Terminate Process and Remain Resident not implemented. | 2021/09/26 |
| Another World | :see_no_evil: Crashes | Fails with unimplemented int 15.6 (Which is weird) | 2021/09/26 |
| Arachnophobia | :see_no_evil: Crashes | Int 10.11 operation "GET INT 1F pointer" not implemented. | 2021/09/26 |
| Arkanoid 2 : Revenge of Doh | :see_no_evil: Crashes | Timer latch read mode not implemented. | 2021/09/26 |
| Cryo Dune | :sunglasses: Fully playable | | 2021/09/26 |
| Double dragon 3 | :see_no_evil: Crashes | Int 10.3 (text mode) not implemented. | 2021/09/26 |
| Dragon's Lair | :see_no_evil: Crashes | Terminates without displaying anything and without error. | 2021/09/26 |
| Dragon's Lair 3 | :see_no_evil: Crashes | Int 10.3 (text mode) not implemented. | 2021/09/26 |
| Dune 2 | :see_no_evil: Crashes | Int 2F not implemented (Himem XMS Driver) | 2021/09/26 |
| F-15 Strike Eagle II | :see_no_evil: Crashes | Launching sub programs not implemented (int 21, 4B) | 2021/09/26 |
| Flight Simulator 5 | :see_no_evil: Crashes | Launching sub programs not implemented (int 21, 4B) | 2021/09/26 |
| Home Alone | :see_no_evil: Crashes | Int 10.8 (text mode) not implemented | 2021/09/26 |
| KGB | :sunglasses: Fully playable | | 2021/09/26 |
| Oliver & Compagnie | :see_no_evil: Crashes | Int 10.11 operation "GET INT 1F pointer" not implemented. | 2021/09/26 |
| Prince of persia | :slightly_smiling_face: Playable | Intro screen is black but if you press space you will go ingame. | 2021/09/26 |
| Plan 9 From Outer Space| :confused: Not playable | Black screen. | 2021/09/26 |
| Quest for glory 3 | :see_no_evil: Crashes | Int 2F not implemented (Himem XMS Driver) | 2021/09/26 |
| SimCity | :see_no_evil: Crashes | Int 10.11 operation "ROM 8x8 double dot pointer" not implemented. | 2021/09/26 |
| Stunts | :slightly_smiling_face: Playable | Works without issue but crashes when you press a key during intro. | 2021/12/10 |
| Space Quest : The Sarien Encounter | :confused: Not playable | Program exits with code 1. | 2021/09/26 |
| Space Quest IV : Roger Wilco and the Time Rippers | :confused: Not playable | Program exits with code 1. | 2021/09/26 |
| Starvega | :see_no_evil: Crashes | Int 10.11 operation "GET INT 1F pointer" not implemented. | 2021/09/26 |
| Super Tetris | :see_no_evil: Crashes | Int 10.8 (text mode) not implemented | 2021/09/26 |
| Top Gun : Danger Zone | :see_no_evil: Crashes | Accesses stdin via dos file API and this is not implemented. | 2021/09/26 |
| Ultima IV : The Quest of the Avatar | :confused: Not playable | Black screen. | 2021/09/26 |