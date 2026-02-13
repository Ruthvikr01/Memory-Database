import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MemDBFullJava
 *
 * - Loads CSV into an in-memory singly linked list (header + node chain) via recursive loader
 * - Exports CSV recursively
 * - Implements bubble, insertion, merge, quick sorts (operate on data rows; header preserved)
 * - Supports a simple SQL-like command:
 *     select c1, c2, c3 from t1 order by c4 ASC with bubble_sort
 * - Menu-driven CLI
 *
 * Note: For sorting, the linked list is converted into ArrayList for easier indexing and algorithm
 * implementation; after sorting, the linked list is rebuilt. Export and load remain recursive.
 */
public class MemDBFullJava {

    // Node for singly linked list
    static class Node {
        String[] row;
        Node next;
        Node(String[] row) { this.row = row; }
    }

    // MemoryDB class
    static class MemoryDB {
        String[] header = null; // column names
        Node head = null;       // head is header node when loaded (head.row = header)
        int rowCount = 0;       // excluding header

        // Recursive loader: reads file into lines, then build node chain recursively
        void loadFromCsvRecursive(String filename) throws IOException {
            List<String> lines = Files.readAllLines(Paths.get(filename));
            if (lines.isEmpty()) {
                header = null;
                head = null;
                rowCount = 0;
                return;
            }
            header = splitCsvLine(lines.get(0));
            // create head node with header
            head = new Node(header);
            rowCount = 0;
            // recursively attach the rest
            head.next = buildNodesRecursive(lines, 1);
            // count rows
            Node cur = head.next;
            while (cur != null) { rowCount++; cur = cur.next; }
        }

        // helper: build linked nodes recursively
        private Node buildNodesRecursive(List<String> lines, int index) {
            if (index >= lines.size()) return null;
            String[] row = splitCsvLine(lines.get(index));
            Node node = new Node(row);
            node.next = buildNodesRecursive(lines, index + 1);
            return node;
        }

        // Export recursively: write starting from head
        void exportToCsvRecursive(String filename) throws IOException {
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(filename))) {
                exportNodeRecursive(head, bw);
            }
        }

        private void exportNodeRecursive(Node node, BufferedWriter bw) throws IOException {
            if (node == null) return;
            bw.write(String.join(",", node.row));
            bw.newLine();
            exportNodeRecursive(node.next, bw);
        }

        // Print all (non-recursive for console convenience)
        void printAll() {
            if (head == null) {
                System.out.println("No data loaded.");
                return;
            }
            Node cur = head;
            int count=0;
            System.out.println("Header: " + Arrays.toString(cur.row));
            cur = cur.next;
            while (cur != null) {
                System.out.println(Arrays.toString(cur.row));
                count++;
                cur = cur.next;
            }
            System.out.println("Total rows (excluding header): " + count);
        }

        // Utility: convert a CSV line to String[] (simple split; assumes no quoted commas)
        private String[] splitCsvLine(String line) {
            // trim and split; keep empty fields
            return Arrays.stream(line.split(",", -1)).map(String::trim).toArray(String[]::new);
        }

        // Convert linked-list data rows (excluding header) to ArrayList<String[]>
        List<String[]> toListRows() {
            List<String[]> rows = new ArrayList<>();
            if (head == null) return rows;
            Node cur = head.next; // skip header
            while (cur != null) {
                rows.add(cur.row);
                cur = cur.next;
            }
            return rows;
        }

        // Rebuild linked list from header + list of rows
        void rebuildFromList(List<String[]> rows) {
            if (header == null) return;
            head = new Node(header);
            Node cur = head;
            for (String[] r : rows) {
                cur.next = new Node(r);
                cur = cur.next;
            }
            rowCount = rows.size();
        }

        // ----- Sorting wrappers: each sorts the data rows list and rebuilds linked list -----

        void bubbleSortByColumns(int[] colOrder, boolean ascending) {
            List<String[]> rows = toListRows();
            int n = rows.size();
            for (int i = 0; i < n-1; i++) {
                boolean swapped = false;
                for (int j = 0; j < n-1-i; j++) {
                    if (compareRows(rows.get(j), rows.get(j+1), colOrder, ascending) > 0) {
                        Collections.swap(rows, j, j+1);
                        swapped = true;
                    }
                }
                if (!swapped) break;
            }
            rebuildFromList(rows);
        }

        void insertionSortByColumns(int[] colOrder, boolean ascending) {
            List<String[]> rows = toListRows();
            for (int i = 1; i < rows.size(); i++) {
                String[] key = rows.get(i);
                int j = i - 1;
                while (j >= 0 && compareRows(rows.get(j), key, colOrder, ascending) > 0) {
                    rows.set(j+1, rows.get(j));
                    j--;
                }
                rows.set(j+1, key);
            }
            rebuildFromList(rows);
        }

        void mergeSortByColumns(int[] colOrder, boolean ascending) {
            List<String[]> rows = toListRows();
            if (!rows.isEmpty()) {
                rows = mergeSortList(rows, colOrder, ascending);
            }
            rebuildFromList(rows);
        }

        private List<String[]> mergeSortList(List<String[]> arr, int[] colOrder, boolean asc) {
            int n = arr.size();
            if (n <= 1) return arr;
            int mid = n/2;
            List<String[]> left = mergeSortList(new ArrayList<>(arr.subList(0, mid)), colOrder, asc);
            List<String[]> right = mergeSortList(new ArrayList<>(arr.subList(mid, n)), colOrder, asc);
            return merge(left, right, colOrder, asc);
        }

        private List<String[]> merge(List<String[]> a, List<String[]> b, int[] colOrder, boolean asc) {
            List<String[]> res = new ArrayList<>(a.size()+b.size());
            int i=0, j=0;
            while (i<a.size() && j<b.size()) {
                if (compareRows(a.get(i), b.get(j), colOrder, asc) <= 0) {
                    res.add(a.get(i++));
                } else {
                    res.add(b.get(j++));
                }
            }
            while (i<a.size()) res.add(a.get(i++));
            while (j<b.size()) res.add(b.get(j++));
            return res;
        }

        void quickSortByColumns(int[] colOrder, boolean ascending) {
            List<String[]> rows = toListRows();
            quickSort(rows, 0, rows.size()-1, colOrder, ascending);
            rebuildFromList(rows);
        }

        private void quickSort(List<String[]> arr, int lo, int hi, int[] colOrder, boolean asc) {
            if (lo >= hi) return;
            int p = partition(arr, lo, hi, colOrder, asc);
            quickSort(arr, lo, p-1, colOrder, asc);
            quickSort(arr, p+1, hi, colOrder, asc);
        }

        private int partition(List<String[]> arr, int lo, int hi, int[] colOrder, boolean asc) {
            String[] pivot = arr.get(hi);
            int i = lo;
            for (int j = lo; j < hi; j++) {
                if (compareRows(arr.get(j), pivot, colOrder, asc) <= 0) {
                    Collections.swap(arr, i, j);
                    i++;
                }
            }
            Collections.swap(arr, i, hi);
            return i;
        }

        // Compare two rows according to column priority order and asc/desc
        // returns negative if a < b, 0 if equal, positive if a > b
        private int compareRows(String[] a, String[] b, int[] colOrder, boolean ascending) {
            for (int col : colOrder) {
                String va = col < a.length ? a[col] : "";
                String vb = col < b.length ? b[col] : "";
                // try numeric compare if both look like ints
                Integer ia = tryParseInt(va);
                Integer ib = tryParseInt(vb);
                int cmp;
                if (ia!=null && ib!=null) {
                    cmp = Integer.compare(ia, ib);
                } else {
                    cmp = va.compareToIgnoreCase(vb);
                }
                if (cmp != 0) {
                    return ascending ? cmp : -cmp;
                }
            }
            return 0;
        }

        private Integer tryParseInt(String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception e) {
                return null;
            }
        }

        // Utility: rebuild linked list preserving header, from given list of selected columns rows
        // (not used directly here but kept for extension)
    } // end MemoryDB

    // ------ SQL-like parser & executor ------

    static class SqlExecutor {
        MemoryDB db;
        SqlExecutor(MemoryDB db) { this.db = db; }

        /**
         * Simple parser for grammar:
         * select col1, col2 from t1 order by colX ASC|DSC with bubble_sort
         *
         * It returns the resulting rows exported to outputFile (full rows, not only selected columns)
         * For simplicity, "from t1" is accepted but ignored (we only have one table).
         */
        void execute(String sql, String outputFile) throws IOException {
            // normalize
            String s = sql.trim();
            if (!s.toLowerCase().startsWith("select ")) throw new IllegalArgumentException("Only SELECT supported.");
            // split into parts: before 'from', after 'from'
            int fromIdx = indexOfWord(s.toLowerCase(), " from ");
            if (fromIdx < 0) throw new IllegalArgumentException("Missing FROM clause.");
            String selectPart = s.substring(7, fromIdx).trim(); // after "select " up to from
            String rest = s.substring(fromIdx + 6).trim(); // after "from "
            // we won't strictly validate table name; find "order by"
            int orderIdx = indexOfWord(rest.toLowerCase(), " order by ");
            if (orderIdx < 0) throw new IllegalArgumentException("Missing ORDER BY clause.");
            String afterFrom = rest.substring(0, orderIdx).trim(); // table name (ignored)
            String afterOrder = rest.substring(orderIdx + 10).trim(); // starts with column name
            // find ASC or DSC and 'with'
            // acceptable: "colX ASC with bubble_sort" or "colX DSC with quick_sort"
            String[] tokens = afterOrder.split("\\s+");
            if (tokens.length < 4) throw new IllegalArgumentException("ORDER BY clause incomplete.");
            String orderCol = tokens[0].trim().replaceAll(",", "");
            String orderDir = tokens[1].trim().toUpperCase();
            if (!(orderDir.equals("ASC") || orderDir.equals("DSC") || orderDir.equals("DESC"))) {
                throw new IllegalArgumentException("ORDER must specify ASC or DSC.");
            }
            // find 'with'
            int withIdx = afterOrder.toLowerCase().indexOf(" with ");
            if (withIdx < 0) throw new IllegalArgumentException("Missing WITH clause (sorting name).");
            String withPart = afterOrder.substring(withIdx + 6).trim();
            String sortName = withPart.split("\\s+")[0].trim().toLowerCase(); // first token after with
            // Determine column indices
            Map<String,Integer> colIndex = headerMap(db.header);
            if (colIndex == null) throw new IllegalStateException("No header loaded.");
            if (!colIndex.containsKey(orderCol)) {
                throw new IllegalArgumentException("ORDER column not found in header: " + orderCol);
            }
            int orderColIdx = colIndex.get(orderCol);
            // Determine selected columns (we will still export full rows, but selection could be used to project)
            // Here selectPart may be "*" or "c1, c2"
            List<String> selectedCols = Arrays.stream(selectPart.split(","))
                    .map(String::trim).filter(x->!x.isEmpty()).collect(Collectors.toList());

            boolean asc = orderDir.equalsIgnoreCase("ASC");
            // Build column order: primary = orderColIdx; we can only order by one column per grammar, but we want tie-breakers
            // Use orderCol, then possibly other columns - but per requirement, order by one column is sufficient.
            int[] colOrder = new int[] { orderColIdx };

            // run the chosen sort
            switch (sortName) {
                case "bubble_sort":
                case "bubble-sort":
                case "bubble":
                    db.bubbleSortByColumns(colOrder, asc);
                    break;
                case "insertion_sort":
                case "insertion-sort":
                case "insertion":
                    db.insertionSortByColumns(colOrder, asc);
                    break;
                case "merge_sort":
                case "merge-sort":
                case "merge":
                    db.mergeSortByColumns(colOrder, asc);
                    break;
                case "quick_sort":
                case "quick-sort":
                case "quick":
                    db.quickSortByColumns(colOrder, asc);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown sorting method: " + sortName);
            }

            // Export final rows to outputFile (full rows). Use recursive exporter from db
            db.exportToCsvRecursive(outputFile);

            System.out.println("SQL executed. Sorted data written to: " + outputFile);
        }

        private int indexOfWord(String src, String wordWithSpaces) {
            return src.indexOf(wordWithSpaces);
        }

        private Map<String,Integer> headerMap(String[] header) {
            if (header == null) return null;
            Map<String,Integer> map = new HashMap<>();
            for (int i=0;i<header.length;i++) map.put(header[i], i);
            return map;
        }
    } // end SqlExecutor

    // ----- Main & Menu -----
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        MemoryDB db = new MemoryDB();
        SqlExecutor exec = new SqlExecutor(db);

        System.out.println("MemDBFullJava - simple in-memory DB toy");
        System.out.println("Make sure you downloaded student-data.csv and know its path.");

        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Load CSV (recursive)");
            System.out.println("2. Run SQL-like command and export");
            System.out.println("3. Export current data (recursive)");
            System.out.println("4. Show data");
            System.out.println("5. Run bubble sort / insertion / merge / quick on default ORDER and export (interactive)");
            System.out.println("6. Exit");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1": {
                        System.out.print("Enter path to CSV file: ");
                        String path = "C:\\Users\\ymall\\IdeaProjects\\javacp\\src\\student-data.csv";
                        db.loadFromCsvRecursive(path);
                        System.out.println("Loaded file. Header: " + Arrays.toString(db.header));
                        System.out.println("Rows loaded: " + db.rowCount);
                        break;
                    }
                    case "2": {
                        System.out.println("Enter SQL-like command:");
                        String sql = sc.nextLine();
                        System.out.print("Enter output filename (path) for sorted CSV: ");
                        String out = sc.nextLine().trim();
                        exec.execute(sql, out);
                        break;
                    }
                    case "3": {
                        System.out.print("Enter output filename (path): ");
                        String out = sc.nextLine().trim();
                        db.exportToCsvRecursive(out);
                        System.out.println("Exported to " + out);
                        break;
                    }
                    case "4": {
                        db.printAll();
                        break;
                    }
                    case "5": {
                        System.out.print("Enter column to order by (exact header name): ");
                        String col = sc.nextLine().trim();
                        System.out.print("ASC or DSC: ");
                        String dir = sc.nextLine().trim();
                        boolean asc = dir.equalsIgnoreCase("ASC");
                        System.out.print("Choose sort: bubble / insertion / merge / quick : ");
                        String sort = sc.nextLine().trim().toLowerCase();
                        // validate header
                        Map<String,Integer> map = new HashMap<>();
                        for (int i=0;i<db.header.length;i++) map.put(db.header[i], i);
                        if (!map.containsKey(col)) {
                            System.out.println("Column not found in header.");
                            break;
                        }
                        int[] colOrder = new int[]{ map.get(col) };
                        switch (sort) {
                            case "bubble": db.bubbleSortByColumns(colOrder, asc); break;
                            case "insertion": db.insertionSortByColumns(colOrder, asc); break;
                            case "merge": db.mergeSortByColumns(colOrder, asc); break;
                            case "quick": db.quickSortByColumns(colOrder, asc); break;
                            default: System.out.println("Unknown sort."); continue;
                        }
                        System.out.print("Enter output filename for sorted CSV: ");
                        String outf = sc.nextLine().trim();
                        db.exportToCsvRecursive(outf);
                        System.out.println("Sorted & exported to " + outf);
                        break;
                    }
                    case "6": {
                        System.out.println("Exiting.");
                        return;
                    }
                    default:
                        System.out.println("Unknown choice.");
                }
            } catch (IOException ioe) {
                System.err.println("I/O error: " + ioe.getMessage());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
