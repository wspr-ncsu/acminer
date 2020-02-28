/*

   vdexExtractor
   -----------------------------------------

   Anestis Bechtsoudis <anestis@census-labs.com>
   Copyright 2017 - 2018 by CENSUS S.A. All Rights Reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

#ifndef _VDEX_BACKEND_010_H_
#define _VDEX_BACKEND_010_H_

#include "../common.h"
#include "../dex.h"
#include "vdex_010.h"

typedef struct __attribute__((packed)) {
  vdexDepStrings_010 extraStrings;
  vdexDepTypeSet_010 assignTypeSets;
  vdexDepTypeSet_010 unassignTypeSets;
  vdexDepClassResSet_010 classes;
  vdexDepFieldResSet_010 fields;
  vdexDepMethodResSet_010 methods;
  vdexDepUnvfyClassesSet_010 unvfyClasses;
} vdexDepData_010;

typedef struct __attribute__((packed)) {
  u4 numberOfDexFiles;
  vdexDepData_010 *pVdexDepData;
} vdexDeps_010;

void vdex_backend_010_dumpDepsInfo(const u1 *);
int vdex_backend_010_process(const char *, const u1 *, size_t, const runArgs_t *);

#endif
