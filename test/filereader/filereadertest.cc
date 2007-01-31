//=========================================================================
//  EVENTLOGTEST.CC - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <stdio.h>
#include <fcntl.h>
#include <io.h>
#include "lcgrandom.h"
#include "filereader.h"

int parseLineNumber(char *line)
{
    return !line || *line == '\r' || *line == '\n' ? -1 : atol(line);
}

int testFileReader(const char *file, long numberOfLines, int numberOfSeeks, int numberOfReadLines)
{
    _setmode(_fileno(stdout), _O_BINARY);
    FileReader fileReader(file);
    LCGRandom random;
    int64 fileSize = fileReader.getFileSize();

    while (numberOfSeeks--) {
        file_offset_t offset = random.next01() * fileSize;
        printf("Seeking to offset: %lld\n", offset);
        fileReader.seekTo(offset);

        int j = numberOfReadLines;
        long expectedLineNumber = -1;
        bool forward;

        while (j--) {
            char *line;

            // either read forward or backward a line
            if (random.next01() < 0.5) {
                line = fileReader.readPreviousLine();

                if (line) {
                    printf("Read previous line: %.*s", fileReader.getLastLineLength(), line);

                    // calculate expected line number based on previous expected if any
                    if (expectedLineNumber != -1) {
                        if (!forward)
                            expectedLineNumber--;
                    }
                    else
                        expectedLineNumber = parseLineNumber(line);
                }
                else {
                    if (expectedLineNumber != -1 &&
                        expectedLineNumber != 0) {
                        printf("*** No more previous lines (%ld) but not at the very beginning of the file\n", expectedLineNumber);
                        return -1;
                    }
                }

                forward = false;
            }
            else {
                line = fileReader.readNextLine();

                if (line) {
                    printf("Read next line: %.*s", fileReader.getLastLineLength(), line);

                    // calculate expected line number based on previous expected if any
                    if (expectedLineNumber != -1) {
                        if (forward)
                            expectedLineNumber++;
                    }
                    else
                        expectedLineNumber = parseLineNumber(line);
                }
                else {
                    if (expectedLineNumber != -1 &&
                        expectedLineNumber != numberOfLines - 1) {
                        printf("*** No more next lines (%ld) but not at the very end of the file\n", expectedLineNumber);
                        return -2;
                    }
                }

                forward = true;
            }

            // compare expected and actual line numbers
            long lineNumber = parseLineNumber(line);
            if (lineNumber != -1 && lineNumber != expectedLineNumber) {
                printf("*** Line number %ld, %ld comparison failed for line: %.*s", lineNumber, expectedLineNumber, fileReader.getLastLineLength(), line);
                return -3;
            }
        }
    }

    return 0;
}

int usage(char *message)
{
    if (message)
        fprintf(stderr, "Error: %s\n\n", message);

    fprintf(stderr, ""
"Usage:\n"
"   filereadertest <input-file-name> <number-of-lines>\n"
);

    return 0;
}

int main(int argc, char **argv)
{
    try {
        if (argc < 5)
            return usage("Not enough arguments specified");
        else {
            int result = testFileReader(argv[1], atol(argv[2]), atoi(argv[3]), atoi(argv[4]));

            if (result)
                printf("FAIL\n");
            else
                printf("PASS\n");

            return result;
        }
    }
    catch (std::exception& e) {
        printf("Error: %s", e.what());
    }
}
