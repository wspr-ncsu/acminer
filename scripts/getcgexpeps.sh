#!/usr/bin/env bash

# A POSIX variable
OPTIND=1 # Reset in case getopts has been used previously in the shell.

indir=$PWD
outfile="cgexpeps.txt"
size=10

USAGE=""
read -r -d '' USAGE <<-'EOF'
	Usage: getcgexpeps [OPTIONS]
	
	Outputs a list of the entry points which have some form of call graph. To
	determine this it finds all files with a size greater than size (default
	is 10M) and retrieves the entry point listed in the comment of the graphml
	file. So this really should only be run on a cg_method debug dir.
	
	Options:
	  -h
	      # Print this message and exit.
	  -o <output_file> [default: ./cgexpeps.txt]
	  -i <input_dir> [default: ./]
	  -s <lower_file_size_bound_in_MB> [default: 10]
	Examples:
	  getcgexpeps
	  getcgexpeps -o path/to/out.txt -i path/to/indir -s 20
EOF

while getopts "h?o:i:s:" opt; do
    case "$opt" in
    h|\?)
        echo "$USAGE"
        exit 0
        ;;
    o)  outfile="$OPTARG"
        ;;
    i)  indir="$OPTARG"
        ;;
	s)  size="$OPTARG"
		;;
    esac
done

> "$outfile"
pattern='^[[:space:]]*Entry[[:space:]]+Point:[[:space:]]+(<.*>)[[:space:]]*$'
find "$indir" -type f -size +"$size"M -exec ls -al {} \; | sort -k 5 -n -r | sed 's/ \+/\t/g' | awk '{ printf "%s\0", $9 }' | while IFS= read -r -d $'\0' file; do
	while IFS= read -r line; do
		if [[ "$line" =~ $pattern ]]; then
			file_size_kb=`du -k "$file" | cut -f1`
			echo "${file_size_kb}kb    ${BASH_REMATCH[1]}    ${file}" >> "$outfile"
			break;
		fi
	done < "$file"
done
