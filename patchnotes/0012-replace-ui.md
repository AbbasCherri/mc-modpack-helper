# 0012 Replace UI

Right click a row, Replace with compatible version. Needs an online match first and a target MC version, which comes from the detector when it can and otherwise from a new MC field in the toolbar. The detected version prefills the field so you can see and override what will be used.

The confirmation dialog spells out the new file, its version, which platform it comes from and where the old jar will be backed up. Search and download both run off the FX thread. After a successful swap the folder rescans itself.
