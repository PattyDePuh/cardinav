var input;
var liste;
var europe_view;
var map;
var vector_layer;
var line_vector_layer;
var result_vector_layer;
var feature_dict = [];
var line_feature_list = [];
var edge_dict = [];
var data_text_input = "";
var result_feature_list = [];
var result_duration = 0;
var result_divergence = 0.0;
var myStyleFunction = function(feature, resolution){
    return [new ol.style.Style({
        stroke: new ol.style.Stroke({
            color: [255,0,0,1],
            width: 3
        })
    })];
};
//Initialisierung des Overlays
var overlay_html = document.createElement("DIV");
overlay_html.setAttribute("class", "canvas_white");
var number_label = document.createElement("SPAN");
number_label.setAttribute("class", "number_red");
var name_label = document.createElement("SPAN");
name_label.setAttribute("class", "name_white");
overlay_html.appendChild(number_label);
overlay_html.appendChild(name_label);
var overlay_canvas = new ol.Overlay({
    element: overlay_html
});

function init(){
    proj4.defs("EPSG:32632", "+proj=utm +zone=32 +ellps=WGS84 +datum=WGS84 +units=m +no_defs");
    makeMap();
    setLayerStyle("Popularität");
    initInteraction();
    console.log("init");
}


function makeMap(){
    console.log("BaseLayer");
    var baseLayer = new ol.layer.Tile({
                source: new ol.source.OSM()
            });
    
    console.log("VectorLayer");
    result_vector_layer = new ol.layer.Vector({
        source: new ol.source.Vector({
            features: result_feature_list
        }),
        style: myStyleFunction
    });
    
    console.log("View");
    var bad_rothen_view = new ol.View({
        zoom: 13,
        projection: ol.proj.get("EPSG:32632")
    });
    
    console.log("Center");
    //Europäischer Mittelpunkt in Geo-Koordinaten
    var center_point = new ol.geom.Point([8.15, 52.11]);
    //console.log(center_point.getCoordinates());
    center_point = center_point.transform(ol.proj.get("EPSG:4326"), ol.proj.get("EPSG:3857"));
    //console.log(center_point.getCoordinates());
    var coords = center_point.getCoordinates(); 
    
    console.log("SetCenter");
    //Transformierter Mittelpunkt in die View setzen.
    bad_rothen_view.setCenter([Math.round( coords[0] ), Math.round( coords[1])]);
    
    //Initialisiere die Karte
    map = new ol.Map({
        layers: [baseLayer, result_vector_layer],
        target: document.getElementById('map'),
        overlays: [overlay_canvas],
        view: bad_rothen_view
    });

    //DragNDrop-FileUpload-Listener
    document.getElementById('upload_panel').addEventListener('drop', function(event){
        event.stopPropagation();
        event.preventDefault();
        var reader = new FileReader();
        reader.onload = function(e) {
            data_text_input = reader.result;
            $("#upload_panel").css("visibility", "hidden"); 
            loadResultGraph();
        };
        reader.readAsText(event.dataTransfer.files[0]);
        console.log(event.dataTransfer.files[0].name); 
    }, true);
    document.getElementById('upload_panel').addEventListener('mouseleave', function(event){        
        console.log("leave");
        $("#upload_panel").css("visibility", "hidden"); 
        event.stopPropagation();
        event.preventDefault();
    }, true);

    //document.getElementById('upload_panel').addEventListener('dragover', function(event){
    document.addEventListener('dragover', function(event){
        console.log("drag");
        $("#upload_panel").css("visibility", "visible"); 
        event.preventDefault();
        event.stopPropagation();
    }, true);
    console.log("Fertig");
}

function setLayerStyle(keyword){
    switch(keyword.value){
        case "Laufgeschwindigkeit":
            myStyleFunction = function(feature, resolution){
                return new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: [255,0,0,0.9],
                        width: 3
                    })
                });
            };
        break;
        case "Steigung":
            myStyleFunction = function(feature, resolution){
                return new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: [255,0,0,0.9],
                        width: 3
                    })
                });
            };
        break;
        case "Höhenmeter":
            myStyleFunction = function(feature, resolution){
                return new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: [255,255,0,0.9],
                        width: 3
                    })
                });
            };
        break;
        case "Popularität":
            myStyleFunction = function(feature, resolution){
                var stroke = new ol.style.Stroke({width:2.0});
                x = parseInt(feature.get("popularity"));
                if(x < 1.0)
                    stroke.setColor([255,0,0,1]);
                else if(x < 2.0)
                    stroke.setColor([255,255,0,1]);
                else if(x < 3.0)
                    stroke.setColor([0,255,0,1]);
                else if(x < 4.0)
                    stroke.setColor([0,255,255,1]);
                else if(x < 5.0)
                    stroke.setColor([0,155,200,1]);
                else
                    stroke.setColor([0,50,255,1]);
                return [new ol.style.Style({
                    stroke: stroke
                })];
            };
        break;
        case "Terrain":
            myStyleFunction = function(feature, resolution){
                return [new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: [255,0,0,0.9],
                        width: 3
                    })
                })];
            };
        break;
    }
    result_vector_layer.setStyle(myStyleFunction);
}


function initInteraction(){
    // Mouseover-Event für Namens-Anzeige
    selector = new ol.interaction.Select({
        condition: ol.events.condition.pointerMove
    });
    map.addInteraction(selector);
    selector.on("select", function(event){
        temp_list = event.target.getFeatures();
        if(temp_list.getLength() != 0){
            if(temp_list.item(0).get("time") != undefined){
                //Knoten-Handling
                number_label.innerHTML = temp_list.item(0).get("popularity");
                name_label.innerHTML = temp_list.item(0).get("milestone");
                coordinate = temp_list.item(0).getGeometry().getCoordinates();
                overlay_canvas.setPosition(coordinate[0]);
            }         
        }
    });

}

function loadResultGraph(){
    console.log("Starte");
    var lines = data_text_input.split("\n");
    var meta_line = lines[0].split("|");
    var anzahl_segmente = parseInt(meta_line[2]);
    result_feature_list.size = 0;
    result_duration = parseInt(meta_line[0]);
    result_divergence =  parseFloat(meta_line[1]);
    var format = new ol.format.WKT();
    //Parse die Kanten
    for(i = 1; i <= anzahl_segmente; i++){
        var focus = lines[i].split("|");
        if(i < anzahl_segmente){
            line = format.readGeometry(focus[9].split(";")[1]);
        }
        //Koordinaten umtransformieren
        line.transform(ol.proj.get("EPSG:32632"), ol.proj.get("EPSG:3857"));
        line_feature = new ol.Feature({
            geometry: line,
            time: parseInt(focus[0]),
            milestone: parseFloat(focus[1]),
            tempo: parseFloat(focus[2]),
            energy: parseFloat(focus[3]),
            divergence: parseFloat(focus[4]),
            height: parseFloat(focus[5]),
            slope: parseFloat(focus[6]),
            popularity: parseFloat(focus[7]),
            terrain: parseFloat(focus[8]),
            style: myStyleFunction
        });
        result_feature_list.push(line_feature);
    }
    result_vector_layer.setSource(new ol.source.Vector({features: result_feature_list}));
}