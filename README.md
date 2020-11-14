# mnfinder

Source code for the Auto-PhotoConverter plugin for micro-manager (https://micro-manager.org, https://github.com/micro-manager/micro-manager). 

This plugin executes image acuqisition on locations defined in Micro-Manager's position list, analyzes the images using pluggable code (modules are included to find all cells, fraction of cells, nuclei of a given size), and illuminates the positive cells (using a DMD or similar).  This plugin was developed for high-content screening of pooled CRISPR libraries (https://www.biorxiv.org/content/10.1101/2020.06.30.179648v1). It started out as a project to find cells with micro-nuclei, hence the (now inappropriate) name.
