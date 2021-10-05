This is a windows exe. To run just do the following:

	SparseConverter.exe /decompress system.img_sparsechunk.0 system.img

Where "system.img_sparsechunk.0" is the first of the sparse files from 0-n and "system.img" is the output file.

To run on linux this needs to be run using mono. So do:

	mono SparseConverter.exe /decompress system.img_sparsechunk.0 system.img

