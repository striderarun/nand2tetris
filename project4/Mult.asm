// i=1
@i
M=1

// sum=R0
@R0
D=M
@sum
M=D

// Initialize R2=0
@R2
M=0

(LOOP)
    // R1-i == 0
    @R1
    D=M
    @i
    D=D-M
    @STOP
    D;JEQ

    // sum = sum + R0
    @R0
    D=M
    @sum
    M=D+M

    //i=i+1
    @i
    M=M+1
    @LOOP
    0;JMP
(STOP)
    @sum
    D=M
    @R2
    M=D
(END)
    @END
    0;JMP









