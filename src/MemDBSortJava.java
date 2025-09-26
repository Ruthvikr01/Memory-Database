import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MemDBSortJava {
        LinkedList<String[]> rows = new LinkedList<>();

        // Load CSV into linked list recursively
        void loadFromCsv(String file) throws IOException {
            rows.clear();
            LinkedList<String> lineList = new LinkedList<>(Files.readAllLines(Paths.get(file)));
            loadRows(lineList);
        }

        private void loadRows(LinkedList<String> lineList) {
            if (lineList.isEmpty()) return;
            String line = lineList.removeFirst();
            rows.add(line.split(","));
            loadRows(lineList);
        }

        // Export into CSV recursively
        void exportToCsv(String file) throws IOException {
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(file))) {
                exportNode(bw, rows, 0);
            }
        }

        private void exportNode(BufferedWriter bw, LinkedList<String[]> list, int index) throws IOException {
            if (index >= list.size()) return;
            String[] row = list.get(index);
            bw.write(String.join(",", row));
            bw.newLine();
            exportNode(bw, list, index + 1);
        }

        // Print all rows
        void printAll() {
            printRowsRecursively(rows.iterator(), 0);
        }

        private void printRowsRecursively(Iterator<String[]> it, int count) {
            if (!it.hasNext()) {
                System.out.println("Total number of rows in Memory DB Toy: " + count);
                return;
            }
            String[] row = it.next();
            System.out.println(Arrays.toString(row));
            printRowsRecursively(it, count + 1);
        }


        // Bubble Sort

        void bubbleSort() {
        for (int i = 1; i < rows.size() - 1; i++) {
            for (int j = 1; j < rows.size() - i; j++) {
                String[] a = rows.get(j);
                String[] b = rows.get(j + 1);

                if (a.length < 3 || b.length < 3) continue;

                boolean swap = false;

                int cmpSchool = a[0].compareToIgnoreCase(b[0]);
                if (cmpSchool > 0) {
                    swap = true;
                } else if (cmpSchool == 0) {
                    int cmpSex = a[1].compareToIgnoreCase(b[1]);
                    if (cmpSex > 0) {
                        swap = true;
                    } else if (cmpSex == 0) {
              
                        try {
                            int ageA = Integer.parseInt(a[2].trim());
                            int ageB = Integer.parseInt(b[2].trim());
                            if (ageA > ageB) swap = true;
                        } catch (NumberFormatException e) {}
                    }
                }

                if (swap) {
                    rows.set(j, b);
                    rows.set(j + 1, a);
                }
            }
        }
    }

   // Insertion Sort
        void insertionSort() {
        for (int i = 2; i < rows.size(); i++) { // start at 2 so header stays
            String[] key = rows.get(i);
            int j = i - 1;
            while (j >= 1 && shouldSwap(rows.get(j), key)) {
                rows.set(j + 1, rows.get(j));
                j--;
            }
            rows.set(j + 1, key);
        }
    }

    private boolean shouldSwap(String[] a, String[] b) {
        if (a.length < 3 || b.length < 3) return false;

        int cmpSchool = a[0].compareToIgnoreCase(b[0]);
        if (cmpSchool > 0) return true;
        if (cmpSchool < 0) return false;

        int cmpSex = a[1].compareToIgnoreCase(b[1]);
        if (cmpSex > 0) return true;
        if (cmpSex < 0) return false;

        try {
            int ageA = Integer.parseInt(a[2].trim());
            int ageB = Integer.parseInt(b[2].trim());
            return ageA > ageB;
        } catch (NumberFormatException e) {
            return false;
        }
    }

        public static void main(String[] args) {
            Scanner sc = new Scanner(System.in);
            MemDBSortJava db = new MemDBSortJava();
            String file = "/home/ruthvik/Downloads/student-data.csv";

            while (true) {
                System.out.println("\n--- Memory DB Toy Menu ---");
                System.out.println("1. Load CSV");
                System.out.println("2. Export CSV");
                System.out.println("3. Show data");
                System.out.println("4. Bubble sort & export");
                System.out.println("5. Insertion sort & export");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");
                int ch = sc.nextInt();
                sc.nextLine();

                try {
                    switch (ch) {
                        case 1:
                            db.loadFromCsv(file);
                            System.out.println("Loaded " + file + " :");
                            db.printAll();
                            break;
                        case 2:
                            System.out.print("Enter output filename(.csv): ");
                            String out = sc.nextLine();
                            db.exportToCsv(out);
                            System.out.println("Exported to " + out);
                            break;
                        case 3:
                            db.printAll();
                            break;
                        case 4:
                            System.out.println("Sorting using Bubble Sort ...");
                            db.bubbleSort();
                            System.out.print("Enter output filename for sorted data(.csv): ");
                            String bubbleOut = sc.nextLine();
                            db.exportToCsv(bubbleOut);
                            System.out.println("Bubble-sorted data exported to " + bubbleOut);
                            break;
                        case 5:
                            System.out.println("Sorting using Insertion Sort ...");
                            db.insertionSort();
                            System.out.print("Enter output filename for sorted data(.csv): ");
                            String insertOut = sc.nextLine();
                            db.exportToCsv(insertOut);
                            System.out.println("Insertion-sorted data exported to " + insertOut);
                            break;
                        case 6:
                            System.out.println("Exiting......");
                            return;
                        default:
                            System.out.println("Invalid choice");
                    }
                } catch (IOException e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
}