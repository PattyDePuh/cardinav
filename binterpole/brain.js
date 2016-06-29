//Variablendeklaration
var height_data_input = "";
var osm_data_input = "";

var xyz_node_output = "";
var edge_output = "";

//Standard "public void main()" ^^
function init(){
    
}


//------------------------------ UTILITY ------------------------------------// 

//Utility-Klasse zur Erstellung von Textdateien, die man dann runterladen kann.
function download(filename, text) {
  var element = document.createElement('a');
  element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
  element.setAttribute('download', filename);

  element.style.display = 'none';
  document.body.appendChild(element);

  element.click();

  document.body.removeChild(element);
}