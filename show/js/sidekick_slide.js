  impress().init();
  slideNavigator.init();

  //Erfasse alle Steps, die parallel zu anderen Steps sichtbar gemacht werden sollen.
  slide_spotlight = [];

  //ID-Angabe der betroffenden Folie, die parallel zu anderen Folien angezeigt werden soll.
  slide_spotlight["vergleich_back"] = ["vergleich0", "vergleich1", "vergleich2", "vergleich3", "vergleich4"];
  
  //Lasse verschiedene Dinge ein-/ausblenden
  var origin_step = null;
  document.addEventListener("impress:stepleave", function(e){
    origin_step = e.target.id;
  });

  document.addEventListener("impress:stepenter", function(e){
    var step_past = origin_step;
    var step_now = e.target.id;
    //Iteriere durch alle Slide_Spotlights
    for( slide in slide_spotlight)
    { 
      was_before = slide_spotlight[slide].indexOf(step_past) != -1
      is_now = slide_spotlight[slide].indexOf(step_now) != -1;
      //Wenn davor Fokus war und jetzt nicht -> hide()
      if(was_before && !is_now){
        document.getElementById(slide).classList.add("hide");
      }
      //Wenn davor kein Fokus war, er aber hinzu kam -> unhide()
      if(!was_before && is_now){
        document.getElementById(slide).classList.remove("hide");
      }
    }
  });