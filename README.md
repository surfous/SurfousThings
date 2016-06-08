# SurfousThings
SmartThings device handlers and SmartApps written or improved upon by surfous. Oh, and a build contraption.

My DTHs are in various states, but all of the built ones are ones I use personally. 

Base DTH code files are under .../build/src/devicetypes (won't work on the platform, guaranteed)
They need to be built with .../build/bin/build *(sorry for the duplicate **build** in the path)* first.

### Building an executable

Building a DTH or SmartApp just takes the unbuilt source file under the ```build/src/``` branch and substitutes in specified code 
'libraries' and snippets found under ```build/src/lib``` and places the result at a path under .../SurfousThings/devicetypes or .../SurfousThings/smartapps
such that they conform to the structure required by the SmartThings IDE GitHub integration.

This allows the same git repo to have the code pieces necessary to assemble a complete executable while only the built executables are exposed to the IDE integration.

To run the build script, you first need to have .../SurfousThings/build/bin in your PATH or use the full relative or absolute path.
Then, simply run it as
```build <PATH_TO_UNBUILT_DTH_OR_SMARTAPP>```

### Versioning

#### Automatically stamping the version in a build

Using the following lines in a block-style comment at the top of an unbuilt executable will result in the built version being 
stamped with a build date and tag to help distinguish and order it against other builds of the same executable. Useful for tracking down issues after 
it's released into the wild:
```
/**
 * My executable comment 
 * blah blah
 *
 *  Date: {{BUILDDATE}}
 *  Build: {{BUILDTAG}}
 */
```

This will result in a comment like this in the built version:
```
/**
 * My executable comment 
 * blah blah
 *
 *  Date: 2016-04-03
 *  Build: 20160404-042508.78526
 */
```

#### Historical builds

Prior builds are not discarded. While they're ignored by the .gitignore files, each prior build is moved aside on the local filesystem, compressed and renamed to include the version.

### Requirements

I can't guarantee this will work anywhere but on MacOS Yosemite or El Capitan

The version of sed with MacOS is anemic. I used homebrew to get GNU sed with the formula **gnu-sed**, and I use it instead. Homebrew installs it as *gsed*, and I call it with that name. 

There may be others that have slipped my memory. I should have written them down earlier. 

### Bugs

I'm sure they're there. 

I know they're there. 

Right now, running build on another machine than the one I developed it on results in an infinite loop rather than a usage message or actually, you know, running the script. 
I hope its just a missing prerequisite thing. 

Otherwise, this is GitHub. You know where [bug reports go](https://github.com/surfous/SurfousThings/issues "issues")

### Wishlist

Let's put these in [issues](https://github.com/surfous/SurfousThings/issues "issues") as well

### Map of directory paths and their variable representation in the build script

```
# Key to the build location variables
# SurfousThings/                                                                                                            $PROJECT_PATH
#               build/
#                     tmp/                                                                                              $PROJECT_TMP_PATH
#                         devicetypes-                                                                                     $ARTIFACT_TYPE
#                         smartapps-                                                                                             "
#                                     name_of_devicetype                                                                   $ARTIFACT_NAME
#                                     name_of_smartapp                                                                           "
#                                                       -#####/
#                         ------------------------------------/                                                            $TMP_BUILD_DIR
# ------------------------------------------------------------/                                                           $TMP_BUILD_PATH
#                                                              name_of_devicetype.src/                                      $ARTIFACT_DIR
#                                                              name_of_smartapp.src/                                             "
# -----------------------------------------------------------------------------------/                               $ARTIFACT_BUILD_PATH
#                                                                                     name_of_devicetype.groovy            $ARTIFACT_FILE
#                                                                                     name_of_smartapp.groovy                    "
# ------------------------------------------------------------------------------------------------------.groovy $ARTIFACT_BUILD_FILE_PATH
#                                                                                     *.bak                                $BAK_FILE_GLOB
# -----------------------------------------------------------------------------------/*.bak                  $ARTIFACT_BAK_FILE_GLOB_PATH
#
#               devicetypes/
#               smartapps/
#                           surfous/                                                                                           $NAMESPACE
# ---------------------------------/                                                                                   $ARTIFACT_DEST_DIR
#                                   name_of_devicetype.src/                                                                 $ARTIFACT_DIR
#                                   name_of_smartapp.src/                                                                        "
# --------------------------------------------------------/                                                           $ARTIFACT_DEST_PATH
#                                                          name_of_devicetype.groovy                                       $ARTIFACT_FILE
#                                                          name_of_smartapp.groovy                                               "
# ---------------------------------------------------------------------------.groovy                             $ARTIFACT_DEST_FILE_PATH  *only for getting the BUILDTAG of existing version
#
```
