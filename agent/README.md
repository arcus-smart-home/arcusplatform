# Arcus Agent

This contains the code that the Iris v2 or Iris V3 hub runs. Version 1 of the hub was unsupported when Iris was running, and is likewise unsupported under Arcus.

# What's missing

Unfortunately not all of the code that powers the v2/v3 hub is open source. Notable missing components include:

* arcus-4g-controller
* arcus-hue-controller
* arcus-zigbee-controller
* arcus-zwave-controller
* arcus-sercomm-controller

These components will need to be evaluated further, and potentially removed or replaced. Since these components are distributed as jar files, it is possible to update parts of the hub without replacing these files short term, but in the interest of a fully open source release, critical components (e.g. zigbee and zwave) should be re-written.
