//Move these to separate place
export const listOfHeaders = [
  "A-IM",
  "Accept",
  "Accept-Charset",
  "Accept-Datetime",
  "Accept-Encoding",
  "Accept-Language",
  "Access-Control-Request-Headers",
  "Access-Control-Request-Method",
  "Authorization",
  "Cache-Control",
  "Connection",
  "Content-Encoding",
  "Content-Length",
  "Content-MD5",
  "Content-Type",
  "Cookie",
  "Date",
  "Expect",
  "Forwarded",
  "From",
  "Host",
  "HTTP2-Settings",
  "If-Match",
  "If-Modified-Since",
  "If-None-Match",
  "If-Range",
  "If-Unmodified-Since",
  "Max-Forwards",
  "Origin",
  "Pragma",
  "Prefer",
  "Proxy-Authorization",
  "Range",
  "Referer",
  "TE",
  "Trailer",
  "Transfer-Encoding",
  "User-Agent",
  "Upgrade",
  "Via",
  "Warning",
  "Upgrade-Insecure-Requests",
  "X-Requested-With",
  "DNT",
  "X-Forwarded-For",
  "X-Forwarded-Host",
  "X-Forwarded-Proto",
  "Front-End-Https",
  "X-Http-Method-Override",
  "X-ATT-DeviceId",
  "X-Wap-Profile",
  "Proxy-Connection",
  "X-UIDH",
  "X-Csrf-Token",
  "X-Request-ID",
  "Save-Data",
  "X-Correlation-ID",
];


export function autocomplete(inputElement, startsWithList, anywhereList) {
  //   the autocomplete function takes three arguments,
  //   'inputElement' :  the text field element 
  //  'startsWithList' : array of autocomplete only for starting with
  //  'anywhereList': array of autocomplete suggestions, to appear after `{`
  if(inputElement.autoComplete){
    return;
  }
  inputElement.autoComplete = this;
  var currentFocus = -1;
  var wordStart = 0,
    wordEnd = 0;
  function updateValue(val) {
    var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
    nativeInputValueSetter.call(inputElement, val);
    var inputEvent = new Event("input", { bubbles: true });
    inputElement.dispatchEvent(inputEvent);
  }

  //   execute a function when someone writes in the text field:
  inputElement.addEventListener("input", function (e) {
    var parentDivElement,
    childDivElement,
      i,
      val = this.value;
    //close any already open lists of autocompleted values
    closeAllLists();
    if (!val) {
      return false;
    }
    currentFocus = 0;
    parentDivElement = document.createElement("DIV");
    parentDivElement.setAttribute("id", this.id + "autocomplete-list");
    parentDivElement.setAttribute("class", "autocomplete-items");
    
    this.parentNode.appendChild(parentDivElement);

    //Add elements with search from starting of input text, used for headers
    for (i = 0; i < startsWithList.length; i++) {
      if (startsWithList[i].substr(0, val.length).toUpperCase() == val.toUpperCase()) {
        childDivElement = document.createElement("DIV");
        childDivElement.innerHTML = "<strong>" + startsWithList[i].substr(0, val.length) + "</strong>";
        childDivElement.innerHTML += startsWithList[i].substr(val.length);
        childDivElement.innerHTML += "<input type='hidden' value='" + startsWithList[i] + "'>";
        childDivElement.addEventListener("click", function (e) {
          updateValue(this.getElementsByTagName("input")[0].value);
          closeAllLists();
        });
        parentDivElement.appendChild(childDivElement);
      }
    }

    //Add Autocomplete values anywhere starting with "{", used for envVars
    wordStart = this.value.substr(0, this.selectionStart).lastIndexOf("{");
    while (wordStart > 0 && this.value[wordStart - 1] == "{") {
      wordStart--;
    }
    wordEnd = this.selectionStart;
    const currentWord = this.value.substr(wordStart, wordEnd - wordStart);

    for (i = 0; i < anywhereList.length; i++) {
      if (anywhereList[i].key.substr(0, wordEnd - wordStart).toUpperCase() == currentWord.toUpperCase()) {
        childDivElement = document.createElement("DIV");
        childDivElement.innerHTML = "<strong>" + anywhereList[i].key.substr(0, currentWord.length) + "</strong>";
        childDivElement.innerHTML += anywhereList[i].key.substr(currentWord.length);
        childDivElement.innerHTML += "<span class='info'>" + anywhereList[i].value.substr(0, 70) + "</span>";
        childDivElement.innerHTML += "<input type='hidden' value='" + anywhereList[i].key + "'>";
        childDivElement.addEventListener("click", function (e) {
          updateValue(inputElement.value.slice(0, wordStart) + this.getElementsByTagName("input")[0].value + inputElement.value.slice(wordEnd));
          closeAllLists();
        });
        parentDivElement.appendChild(childDivElement);
      }
    }
    addActive(parentDivElement.getElementsByTagName("div"));
  });

  // when Autocomplete is open, navigate between items for displaying a Active item
  inputElement.addEventListener("keydown", function (e) {
    var autoCompleteList = document.getElementById(this.id + "autocomplete-list");
    if (autoCompleteList) autoCompleteList = autoCompleteList.getElementsByTagName("div");
    if (e.keyCode == 40) { //Down key, focus to be next element
      currentFocus++;
      addActive(autoCompleteList);
    } else if (e.keyCode == 38) { //Up key, focus will be on prev element
      currentFocus--;
      addActive(autoCompleteList);
    } else if (e.keyCode == 13) { //Enter, select current active key
      e.preventDefault();
      if (currentFocus > -1) {
        if (autoCompleteList) autoCompleteList[currentFocus].click();
      }
    }
  });
  function addActive(listElement) {
    if (!listElement) return false;
    removeActive(listElement);
    if (currentFocus >= listElement.length) currentFocus = 0;
    if (currentFocus < 0) currentFocus = listElement.length - 1;
    // add class "autocomplete-active":
    listElement[currentFocus].classList.add("autocomplete-active");
  }
  function removeActive(listElement) {
    for (var i = 0; i < listElement.length; i++) {
      listElement[i].classList.remove("autocomplete-active");
    }
  }
  function closeAllLists(elmnt) {
    // close all autocomplete lists in the document,
    //  except the one passed as an argument:
    var autoCompleteList = document.getElementsByClassName("autocomplete-items");
    for (var i = 0; i < autoCompleteList.length; i++) {
      if (elmnt != autoCompleteList[i] && elmnt != inputElement) {
        autoCompleteList[i].parentNode.removeChild(autoCompleteList[i]);
      }
    }
  }
  // execute a function when someone clicks in the document:
  document.addEventListener("click", function (e) {
    closeAllLists(e.target);
  });
}
