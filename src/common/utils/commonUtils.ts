/** Move other common utility functions to here from httpClient/apiCatalog */

function downloadAFileToClient(fileName: string, data: any) {
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);
  document.body.appendChild(link);
  link.click();
}

const commonUtils = {
  downloadAFileToClient,
};
export default commonUtils;
