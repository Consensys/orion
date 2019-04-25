title: Dependencies
description: Orion dependencies  
<!--- END of page meta data -->

# Dependencies

## libsodium

Orion requires the [Sodium crypto library](https://download.libsodium.org/doc/) (libsodium) to provide the encryption 
primitives.
 
### Install libsodium

#### MacOS

Install using [homebrew](https://brew.sh/):
```bash
brew install libsodium
```

#### Linux

Download the [latest stable version](https://download.libsodium.org/libsodium/releases/LATEST.tar.gz) 
of libsodium.
 
Execute:
``` bash
./configure
make && make check
sudo make install
```

#### Windows

Perform the following steps:
1. Download the [latest stable **msvc** build](https://download.libsodium.org/libsodium/releases/) of libsodium.
1. Extract the libsodium archive
1. Navigate to the current OS architecture (win32 or x64) directory
1. Navigate to the _Release_ directory
1. Navigate to the relevant version directory (v120 for Windows 8.1, v140+ for Windows 10)
1. Navigate to the _dynamic_ directory
1. Copy the _libsodium.dll_ file to a directory that is on the system path.  eg: `C:\Windows\System32\`

#### Other Systems

Refer to the [libsodium installation docs](https://download.libsodium.org/doc/installation/). 

## LevelDB

### Windows

Windows _may_ require the Microsoft Visual C++ 2010 Redistributable Package to utilise LevelDB.  The distributables are available here:

* [32 bit](https://www.microsoft.com/en-au/download/details.aspx?id=5555)
* [64 bit](https://www.microsoft.com/en-au/download/details.aspx?id=14632)
