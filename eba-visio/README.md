# Environment Build Automation (EBA) - Visio Stencil and Export Tool

Environment Build Automation is using the definition of an environment from a Visio drawing to automatically construct that environment in the cloud. 

## Requirements

The Visio Stencil (src/VisioStencil.vss) and Visio Export Tool(/src/VisioExportToXMLTool.vsd) are compatible with Microsoft Office Visio 2010 and later. A version of Microsoft Excel (auto detected) is also required.

## Purpose

The Visio Stencil (src/VisioStencil.vss) can be used to define a physical operational model for network that can be realised using EBSA Environment Build Automation tools(/docs/index.html).
The companion Visio Export Tool(/src/VisioExportToXMLTool.vsd) will export a physical operational model, from a Visio design created using the stencil provided in this project, as an xml file conforming to the EBSA provider agnostic data model.
The xml can then be passed as an input to the EBSA Environment Build Automation - ACloud Client tool that can interact with your cloud provider to provision your physical design in the cloud.

## Licence

See the LICENCE (/LICENCE) file.

## Documentation

The user guide for the Visio stencil and export tool can be found in the docs folder of this project. See ./docs/index.html.
