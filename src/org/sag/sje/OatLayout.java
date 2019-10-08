/* OAT 127  - Android 8.1.0
 * 
 * Unsigned ints are all little-endian (i.e. right to left)
 *
 * ++++ Oat Header ++++                     
 *   - Size = 4 * 19 + "Key Value Store Size" = 76 + "Key Value Store Size"
 *   - All offsets are from the beginning of the oat header start (i.e. oat_header_offset + OFFSET_VALUE = data location)
 *   - All of the fields are located in the "OatHeader" class in "art/runtime/oat.h"
 *   - The order of the fields in the class appears to be the order in which they are included in the oat file
 *   - They are written in the "OatWriter::WriteHeader" in "art/compiler/oat_writer.cc", which appears to do a memory dump on the object
 *   - Therefore fields should be in the order they are declared in the "OatHeader" class
 *   - To figure out the fields written, ordering, and size of each entry look at the "OatWriter::OatDexFile::Write" method in "art/compiler/oat_writer.cc"
 *   - https://android.googlesource.com/platform/art/+log/master/runtime/oat.h (whenever the oat format changes the kOatVersion field is incremented, 
 *     the files changed along with this change tells us what version of oat contains the change)
 *
 *   ++ Oat Header Breakdown ++
 *     Magic                                      - 4 bytes in left to right char array (always "oat\n")
 *     Oat Version                                - 4 bytes in left to right char array
 *     Adler32 Checksum                           - 4 bytes unsigned int
 *     Instruction Set Flag                       - 4 bytes unsigned int (i.e. arm, arm64, etc.)
 *     Instruction Set Features Bitmap            - 4 bytes unsigned int
 *     Dex File Count                             - 4 bytes unsigned int (i.e. the number of dex files in this oat file)
 *     Oat Dex Files Offset                       - 4 bytes unsigned int (i.e. the location of the dex file names like "/system/framework/core-oj.jar")
 *                                                - added in OAT 127
 *                                                - The file names used to be located right after the end of the oat header (i.e. oat_header_offset + header size)
 *                                                - Now it is here (i.e. oat_header_offset + oat_dex_files_offset)
 *     Executable Offset                          - 4 bytes unsigned int
 *     Interpreter To Interpreter Bridge Offset   - 4 bytes unsigned int
 *     Interpreter To Compiled Code Bridge Offset - 4 bytes unsigned int
 *     Jni Symbol Lookup Offset                   - 4 bytes unsigned int
 *     Quick Generic Jni Trampoline Offset        - 4 bytes unsigned int
 *     Quick IMT Conflict Trampoline Offset       - 4 bytes unsigned int
 *     Quick Resolution Trampoline Offset         - 4 bytes unisnged int
 *     Quick To Interpreter Bridge Offset         - 4 bytes unsigned int
 *     Image Patch Delta                          - 4 bytes signed int
 *     Image File Location Oat Checksum           - 4 bytes unsigned int
 *     Image File Location Oat Data Begin         - 4 bytes unsigned int
 *     Key Value Store Size                       - 4 bytes unsinged int
 *     Key Value Store                            - Variable length of size "Key Value Store Size" (This is where the bootclasspath is stored along with arguments and options used to create the OAT file)
 *
 * ++++ Oat Dex File Entries ++++
 *   - The number of entries is equal to the number of dex files in the oat file (i.e. the "Dex File Count" in the Oat Header) 
 *   - All of the fields are located in the "OatDexFile" class in "art/runtime/oat_file.h"
 *   - To figure out the fields written, ordering, and size of each entry look at the "OatWriter::OatDexFile::Write" method in "art/compiler/oat_writer.cc"
 *   - https://android.googlesource.com/platform/art/+log/master/runtime/oat.h (whenever the oat format changes the kOatVersion field is incremented, 
 *     the files changed along with this change tells us what version of oat contains the change)
 *
 *   ++ Single Oat Dex File Entry ++
 *     File Name Length           - 4 bytes unsigned int
 *     File Name                  - Variable length char array of size "File Name Length"
 *     Checksum                   - 4 bytes unsigned int
 *     Dex Struct Offset          - 4 bytes unsigned int
 *     Class Offsets              - 4 bytes unsigned int
 *     Type Lookup Offset         - 4 bytes unsigned int
 *     Dex Sections Layout Offset - 4 bytes unsigned int (added in OAT 131)
 *     Method BSS Mapping Offset  - 4 bytes unsigned int (added in OAT 127)
 */
