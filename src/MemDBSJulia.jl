# MemDBSJulia.jl
using Printf

# Linked List Node 
mutable struct Node
    row::Vector{String}
    next::Union{Node, Nothing}
end

# MemoryDB
mutable struct MemoryDB
    head::Union{Node, Nothing}
end

MemoryDB() = MemoryDB(nothing)

# Load CSV recursively

function load_from_csv!(db::MemoryDB, file::String)
    lines = readlines(file)
    db.head = nothing
    if !isempty(lines)
        db.head = load_rows(lines, 1)
    end
end

function load_rows(lines::Vector{String}, i::Int)::Union{Node,Nothing}
    if i > length(lines)
        return nothing
    end
    row = split(lines[i], ",")
    return Node(row, load_rows(lines, i + 1))
end

# Export CSV recursively

function export_to_csv(db::MemoryDB, file::String)
    open(file, "w") do io
        export_node(io, db.head)
    end
end

function export_node(io, n::Union{Node,Nothing})
    n === nothing && return
    println(io, join(n.row, ","))
    export_node(io, n.next)
end

# Print all the data

function print_all(db::MemoryDB)
    count = print_nodes_recursive(db.head, 0)
    println("Total number of rows in Memory DB Toy: $count")
end

function print_nodes_recursive(n::Union{Node,Nothing}, count::Int)
    if n === nothing
        return count
    end
    println(n.row)
    return print_nodes_recursive(n.next, count + 1)
end

# Sorting helper

function should_swap(a::Vector{String}, b::Vector{String})
    if length(a) < 3 || length(b) < 3
        return false
    end

    cmp_school = lowercase(a[1]) > lowercase(b[1]) ? 1 : (lowercase(a[1]) < lowercase(b[1]) ? -1 : 0)
    if cmp_school > 0
        return true
    elseif cmp_school < 0
        return false
    end

    cmp_sex = lowercase(a[2]) > lowercase(b[2]) ? 1 : (lowercase(a[2]) < lowercase(b[2]) ? -1 : 0)
    if cmp_sex > 0
        return true
    elseif cmp_sex < 0
        return false
    end

    try
        age_a = parse(Int, strip(a[3]))
        age_b = parse(Int, strip(b[3]))
        return age_a > age_b
    catch
        return false
    end
end

# Bubble Sort

function bubble_sort!(db::MemoryDB)
    if db.head === nothing || db.head.next === nothing
        return
    end

    header = db.head 
    curr_head = db.head.next

    swapped = true
    while swapped
        swapped = false
        prev = header
        curr = curr_head
        while curr !== nothing && curr.next !== nothing
            if should_swap(curr.row, curr.next.row)
                swapped = true
                nxt = curr.next
                curr.next = nxt.next
                nxt.next = curr
                prev.next = nxt
                prev = nxt
            else
                prev = curr
                curr = curr.next
            end
        end
        curr_head = header.next
    end
end


# Insertion Sort
function insertion_sort!(db::MemoryDB)
    if db.head === nothing || db.head.next === nothing
        return
    end

    header = db.head
    sorted = nothing
    curr = header.next

    while curr !== nothing
        next_node = curr.next
        # Insert at head if sorted is empty or curr should come before the first node
        if sorted === nothing || should_swap(sorted.row, curr.row)
            curr.next = sorted
            sorted = curr
        else
            search = sorted
            # Move until the next node is greater than curr
            while search.next !== nothing && !should_swap(search.next.row, curr.row)
                search = search.next
            end
            curr.next = search.next
            search.next = curr
        end
        curr = next_node
    end

    header.next = sorted
    db.head = header
end




function main()
    db = MemoryDB()
    file = "/home/ruthvik/Downloads/student-data.csv"

    while true
        println("\n--- Julia Memory DB Toy Menu ---")
        println("1. Load CSV")
        println("2. Export CSV")
        println("3. Show data")
        println("4. Bubble sort & export")
        println("5. Insertion sort & export")
        println("6. Exit")
        print("Enter choice: ")
        choice = tryparse(Int, readline())
        choice === nothing && continue
        try
            if choice == 1
                load_from_csv!(db, file)
                println("Loaded $file :")
                print_all(db)
            elseif choice == 2
                print("Enter output filename(.csv): ")
                out = readline()
                export_to_csv(db, out)
                println("Exported to $out")
            elseif choice == 3
                print_all(db)
            elseif choice == 4
                println("Sorting using Bubble Sort...")
                bubble_sort!(db)
                print("Enter output filename for sorted data(.csv): ")
                out = readline()
                export_to_csv(db, out)
                println("Bubble-sorted data exported to $out")
            elseif choice == 5
                println("Sorting using Insertion Sort...")
                insertion_sort!(db)
                print("Enter output filename for sorted data(.csv): ")
                out = readline()
                export_to_csv(db, out)
                println("Insertion-sorted data exported to $out")
            elseif choice == 6
                println("Exiting…")
                break
            else
                println("Invalid choice")
            end
        catch e
            @warn "Error: $e"
        end
    end
end

main()