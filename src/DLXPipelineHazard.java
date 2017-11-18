
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class DLXPipelineHazard {

    static int instrCount;
    static String[] instructions;
    static String[][] opcode;
    static String[][] operandsList;
    static String[][] dependentInstr;
    static int[][] nondependentInstructions;
    static String hazardType;
    static int dependentCount;
    static int clock;
    static int ndc;
    static boolean _2conslworsw;
    static int prevCycle;
    static String label;
    static boolean controlHazard;
    static int branchInstruction;
    static Scanner scan;
    static File file;
    static DLXPipelineHazard dlx = new DLXPipelineHazard();
    boolean isDataHazard;

    /**
     * Description : Main method of the class. Accepts file name as input and
     * check the dependency between registers. And selects between data and
     * control hazard
     */
    public static void main(String[] args) {
        try {

            String inputFile = "";
            if (args.length == 0) {
                inputFile = "DataHazard.txt";
            } else {
                if (args[0].contains(".txt")) {
                    inputFile = args[0];
                } else {
                    inputFile = args[0] + ".txt";
                }
            }
            /* Initializing arrays for dpendent and non-dpendent instructions */
            dependentInstr = new String[100][4];
            nondependentInstructions = new int[100][2];

            /* Reading the input file line by line */
            file = new File(inputFile);
            scan = new Scanner(file);

            while (scan.hasNextLine()) {
                instrCount++;
                scan.nextLine();
            }
            scan.close();

            instructions = new String[instrCount + 2];
            opcode = new String[instrCount + 1][1];
            operandsList = new String[instrCount + 1][3];

            Scanner instrScan = new Scanner(file);
            for (int i = 1; i <= instrCount; i++) {
                if (instrScan.hasNext()) {
                    instructions[i] = instrScan.nextLine();
                    decodeInstruction(i, instructions[i]);
                }
            }

            /* Hazard recovery based on detected hazard */
            if (!controlHazard) {
                dlx.dataHazard();
            } else {
                dlx.controlHazard();
            }

        } catch (FileNotFoundException ex) {
            System.out.println("Exception : " + ex.getMessage());
        }
    }

    /*
     * Method Name : decodeInstruction
     * Parameters  : Instruction number, Instruction
     * Description : Method decodes Instruction to find the opcode and operands
     */
    private static void decodeInstruction(int i, String instr) {
        /* Finding if Control Hazard exists */
        if (instr.contains("BEQ") || instr.contains("BNE")) {
            controlHazard = true;
            branchInstruction = i;
        }
        if (instr.contains(":")) {
            label = instr.split(":")[0];
            instr = instr.split(":")[1];
        }
        String[] line = instr.split(" ");
        opcode[i][0] = line[0];
        boolean set = dlx.validateInstruction(opcode[i][0]);
        if (!set){
            System.out.println("\nInstruction - " + instructions[i] + " not of class I-type or J-type");
            System.exit(0);
        }
        operandsList[i] = line[1].split(",");
    }

    /*
     * Method Name : checkDependency
     * Parameters  : -NONE-
     * Description : Method checks the dependency of each instruction with others
     *               with window size of next 3 instructions wrt to operands(registers)
     */
    private void checkDependency() {
        String dependentOperand = "";
        int val = 0;
        String x = "";
        dependentCount = 0;
        ndc = 0;
        /* Looping with each instructions compared to its successor instructions 
         with in the window of that instruction to detect the hazard */
        for (int i = 1; i <= instrCount; i++) {
            for (int l = i + 1; l <= instrCount; l++) {
                for (int j = 0; j < 3; j++) {
                    if ((l) <= instrCount && (j + 2) < 3) {
                        if (operandsList[i][j].equals(operandsList[l][j + 1])) {
                            dependentOperand = operandsList[i][j];
                            if (j == 0 && j + 1 == 1) {
                                hazardType = "RAW";
                                if (opcode[i][0].equalsIgnoreCase("lw") || (opcode[i][0].equalsIgnoreCase("sw"))) {
                                    x = "MEM";
                                } else {
                                    x = "WB";
                                }
                            }
                        } else if (operandsList[l].length == 3 && operandsList[i][j].equals(operandsList[l][j + 2])) {
                            dependentOperand = operandsList[i][j];
                            if (j == 0 && j + 2 == 0) {
                                System.out.println("No Dependency btw " + i + " and " + l);
                            } else if (j == 0 && j + 2 == 1) {
                                hazardType = "RAW";
                            } else if (j == 0 && j + 2 == 2) {
                                hazardType = "RAW";
                            }
                        } else if (operandsList[l].length == 2 && operandsList[i][j].equals(operandsList[l][j + 1])) {
                            _2conslworsw = true;
                        } else if (l - i < 3) {
                            nondependentInstructions[ndc][0] = i;
                            nondependentInstructions[ndc][1] = l;
                            ndc++;
                        }

                        if (hazardType != null && (l - i) < 4) {
                            dependentInstr[dependentCount][0] = String.valueOf(i);
                            dependentInstr[dependentCount][1] = String.valueOf(l);
                            dependentInstr[dependentCount][2] = hazardType;
                            if ((opcode[i][0].equalsIgnoreCase("LW") || opcode[i][0].equalsIgnoreCase("SW")) && (l - i) == 1) {
                                dependentInstr[dependentCount][3] = " Stalling required ";
                                clock++;
                                if (_2conslworsw) {
                                    clock--;
                                    _2conslworsw = false;
                                }
                            } else if (l - i == 2 || l - i == 1) {
                                dependentInstr[dependentCount][3] = " Forwarding required ";
                            } else if (l - i == 3) {
                                dependentInstr[dependentCount][3] = " No action required - operations in order ";
                            }
                            dependentCount++;
                            hazardType = null;
                            val = 0;
                        }
                    }
                }
            }
        }
    }

    /*
     * Method Name : dataHazard
     * Parameters  : -NONE-
     * Description : Method implemented for Data Hazard
     */
    private void dataHazard() {
        isDataHazard = true;
        writePipelinedInstructions();
        calculateCycles();
        checkDependency();
        System.out.println("\nEnter the choice of algorithm : \n 1  - Forwarding \n 2 - Compiler Instruction re-ordering");
        scan = new Scanner(System.in);
        int choice = scan.nextInt();
        if (choice == 1) {
            System.out.println("\nAlgorithm Selected - Forwarding\n");
            writeDependencies();
        } else if (choice == 2) {
            System.out.println("\nAlgorithm Selected - Compiler Instruction re-ordering\n");
            compilerInstructionReordering();
        } else {
            System.out.println("\nDefault Selected - Forwarding\n");
            writeDependencies();
        }
    }

    /*
     * Method Name : controlHazard
     * Parameters  : -NONE-
     * Description : Method implemented for Control Hazard
     */
    private void controlHazard() {
        System.out.println("Control Hazard Exists");
        System.out.println("\nConsidering Optimized Datapath selected");

        System.out.println("\nConsidering Branch taken");
        writePipelinedInstructions();
        calculateCycles();
        checkDependency();
        writeDependencies();
        System.out.println("Clock Cycles with branch instruction taken : " + (clock + 3) + " times the loop count");

        System.out.println("\nConsidering Branch Not taken");
        writePipelinedInstructions();
        calculateCycles();
        checkDependency();
        writeDependencies();
        System.out.println("Clock Cycles with branch instruction not taken : " + clock);

        System.out.println("\nConsidering Branch Delay Slot Scheduling algorithm selected");
        branchDelay();
        calculateCycles();
        checkDependency();
        writeDependencies();
        System.out.println("\nClock Cycles : " + (clock) + " times the loop count");
    }

    /*
     * Method Name : writePipelinedInstructions
     * Parameters  : -NONE-
     * Description : Method writes the piplined instruction
     */
    private void writePipelinedInstructions() {
        System.out.println("\nPIPELINIED INSTRUCTIONS");
        String str = "";
        str = "IF\tID\tEX\tMEM\tWB";
        String next = "";
        String space = "";
        for (int i = 1; i <= instrCount; i++) {
            if (i == 1) {
                next = "";
            } else {
                next = "\t";
            }
            space = space + next;
            str = space.concat(str);
            space = "";
            System.out.println(str);
        }
    }

    /*
     * Method Name : writeInstructionsWithStall
     * Parameters  : -NONE-
     * Description : Method writes the piplined instruction after forwarding 
     * or instruction re-ordering with or without stalls
     */
    private void writeInstructionsWithStall() {
        String str = "";
        str = "IF\tID\tEX\tMEM\tWB";
        String next = "";
        String space = "";
        String pr = "";
        for (int i = 1; i < instrCount + 1; i++) {
            if (i == 1) {
                next = "";
                space = space + next;
                str = space.concat(str);
            } else {
                if (pr.contains("STALL")) {
                    next = "\t";
                    str = "IF\tID\tEX\tMEM\tWB";
                    space = space + next;
                    str = space.concat(str);
                } else {
                    next = "STALL\tSTALL\t";
                    str = "\tIF\t";
                    space = space + next;
                    str = str.concat(space).concat("ID\tEX\tMEM\tWB");
                }
            }
            pr = str;
            System.out.println((opcode[i][0]) + "\t:\t" + str);
            str = "";
            space = "";
        }
    }

    /*
     * Method Name : writeDependencies
     * Parameters  : -NONE-
     * Description : Method writes the piplined instruction dependencies between
     * each instruction within the window size
     */
    private void writeDependencies() {
        for (int i = 0; i < dependentCount; i++) {
            if (dependentInstr[i][0] != null) {
                System.out.println((i + 1) + "  | Instruction " + dependentInstr[i][1] + " dependent on instruction " + dependentInstr[i][0] + "- Data Hazard - classification : " + dependentInstr[i][2] + " Action : " + dependentInstr[i][3]);
            }
        }
        if (isDataHazard) {
            System.out.println("\nNo. of clock Cycles - " + clock);
        }
    }

    /*
     * Method Name : calculateCycles
     * Parameters  : -NONE-
     * Description : Method calculates no. of cycles needed for execution w/o any stalling and forwarding
     */
    private void calculateCycles() {
        clock = 5 + (instrCount - 1);
    }

    /*
     * Method Name : compilerInstructionReordering
     * Parameters  : -NONE-
     * Description : Method implements compiler instruction re-ordering for Data Hazard
     */
    private void compilerInstructionReordering() {
        int c = 0;
        for (int i = 0; i < ndc; i++) {
            boolean isTrue = checkBlock(nondependentInstructions[i][1]);
            if (isTrue) {
                prevCycle = clock;
                System.out.println("\nInstructions " + nondependentInstructions[i][1] + " and " + nondependentInstructions[i][0] + " can be re-ordered\n");
                String str = instructions[nondependentInstructions[i][1]];
                instructions[nondependentInstructions[i][1]] = instructions[nondependentInstructions[i][0]];
                decodeInstruction(nondependentInstructions[i][1], instructions[nondependentInstructions[i][1]]);
                instructions[nondependentInstructions[i][0]] = str;
                decodeInstruction(nondependentInstructions[i][0], instructions[nondependentInstructions[i][0]]);
                writeInstructions();
                calculateCycles();
                checkDependency();
                writeDependencies();
                if (clock >= prevCycle) {
                    compilerInstructionReordering();
                } else {
                    break;
                }
            } else {
                c++;
            }
            if (c == ndc) {
                System.out.println("Compiler Instruction re-ordering not possible");
            }
        }
    }

    /*
     * Method Name : findDependencyinWindow
     * Parameters  : -NONE-
     * Description : Method executes set of instructions within the window to check dependency.
     */
    private static boolean findDependencyinWindow(int dep) {
        for (int i = 0; i < dependentCount; i++) {
            if (dependentInstr[i][1].equalsIgnoreCase(String.valueOf(dep))) {
                System.out.println(dependentInstr[i][1] + " " + dependentInstr[i][0]);
            }
        }
        return true;
    }

    /*
     * Method Name : branchDelay
     * Parameters  : -NONE-
     * Description : Method implements branch delay slot for Control Hazard
     */
    private void branchDelay() {
        if (instructions[branchInstruction + 1] != null) {
            String instr = instructions[branchInstruction + 1];
            instructions[branchInstruction + 1] = instructions[branchInstruction];
            instructions[branchInstruction] = instr;
        } else {
            String instr = instructions[branchInstruction - 1];
            instructions[branchInstruction - 1] = instructions[branchInstruction];
            instructions[branchInstruction] = instr;
        }
        writePipelinedInstructions();
    }

    /*
     * Method Name : writeInstructions
     * Parameters  : -NONE-
     * Description : Method writes the input instruction with or without modifying it
     */
    private void writeInstructions() {
        System.out.println("\nDLX INSTRUCTIONS");
        for (int i = 1; i <= instrCount; i++) {
            System.out.println(i + "|" + instructions[i]);
        }
    }

    /*
     * Method Name : checkBlock
     * Parameters  : Instruction
     * Description : Method implemented for use by compiler instruction 
     * re-ordering for Data Hazard to find if a specific instruction is dependent
     * on any other instruction in the specified instruction window
     */
    private boolean checkBlock(int i) {
        String val1 = String.valueOf(i);
        for (int k = 0; k < dependentCount; k++) {
            for (int l = 0; l < 4; l++) {
                if (val1.equalsIgnoreCase(dependentInstr[l][0]) || val1.equalsIgnoreCase(dependentInstr[l][1])) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Method Name : validateInstruction
     * Parameters  : Instruction
     * Description : Method implemented for validation of instructions to be 
     * of I-type or J-Type 
     */
    private boolean validateInstruction(String instr) {
        String[] instrType = {"ADD", "SUB", "AND", "OR", "XOR", "LW", "SW", "BE", "BNE"};
        for (int i = 0; i < instrType.length; i++) {
            if (instr.toUpperCase().contains(instrType[i])) {
                return true;
            }
        }
        return false;
    }
}
