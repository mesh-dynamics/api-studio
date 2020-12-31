/** Move other common utility functions to here from httpClient/apiCatalog */

function downloadAFileToClient(fileName: string, data: any) {
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);
  document.body.appendChild(link);
  link.click();
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

const commonUtils = {
  downloadAFileToClient,
  getFormattedDate,
};

export default commonUtils;
