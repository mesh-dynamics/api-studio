/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Move other common utility functions to here from httpClient/apiCatalog */
/** global PLATFORM_ELECTRON */

import { remote, fs } from '../../common/helpers/ipc-renderer';

function downloadAFileToClient(fileName: string, data: any) {
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);
  document.body.appendChild(link);
  link.click();
}

function writeToClient(fileName: string, data: any) {
  fs.writeFile(fileName, data, (err: any) => {
    if (err) {
      console.log('Cancelled downloading file', err.message);
      return;
    }

    console.log('File created successfully');
  });
}

function exportServiceConfigToClient(fileName: string, data: any) {
  if(PLATFORM_ELECTRON) {
    const dialog = remote.dialog;
    const windowReference = remote.getCurrentWindow();

    const options = {
      title: "Export Service Config",
      defaultPath: fileName,
      buttonLabel: "Save",
      filters: [{ name: "JSON", extensions: ["json"] }]
    };

    dialog
      .showSaveDialog(windowReference, options)
      .then(result => {
        const fileNameFromDialog = result.filePath;
        if(fileNameFromDialog === undefined) {
          writeToClient(fileName, data);
        } else {
          writeToClient(fileNameFromDialog, data);
        }
      });
  } else {
    downloadAFileToClient(fileName, data);
  }
}

function getFormattedDate(date: any) {
  // TODO: fix this with something more appropriate whenever possible
  let year = date.getFullYear();

  let month = (1 + date.getMonth()).toString();
  month = month.length > 1 ? month : '0' + month;

  let day = date.getDate().toString();
  day = day.length > 1 ? day : '0' + day;

  return month + '/' + day + '/' + year;
}

function isCSVMimeType(type:string){
  return ["text/csv", "application/x-csv", "application/csv", "text/x-comma-separated-values", "text/comma-separated-values", "application/vnd.ms-excel"].indexOf(type.toLowerCase().trim())> -1;
}

const commonUtils = {
  exportServiceConfigToClient,
  downloadAFileToClient,
  getFormattedDate,
  isCSVMimeType
};

export default commonUtils;