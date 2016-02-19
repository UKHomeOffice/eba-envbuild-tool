# EBA - Environment Build Automation

This is a collection of tools to aid creating environments in the Cloud from Visio diagrams 

## Structure
This repo has 3 directories, eba-platform, eba-acloud and eba-visio. Environments are designed in Visio using the stencil in eba-visio/src/VisioStencil.vss, then exported to XML by the export tool in eba-visio/src/VisioExport.vsd. This XML is then passed to the eba-acloud tool.
Tools for managing multiple environments are in eba-platform

Please see the readme files in /eba-acloud, /eba-visio and /eba-platform for more details.


## Licence

Please contact the Home Office for details.

## Documentation

The user guide for the Visio stencil and export tool can be found in the docs folder at ./eba-visio/docs/index.html

The user guide for the EBA ACloud tool can be found in the docs folder at ./eba-acloud/docs/index.html

The documentation for eba-platform can be found in the docs folder at ./eba-platform/docs
