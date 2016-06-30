//Globale Einstellungen
Chart.defaults.global.defaultFontSize = 22;

//
//Todes PieChart
var death_pie_container = $("#death_pie_chart");
var death_pie_data = {
    labels: [
        "Herz-/Kreislauf",
        "Böse Neubildung (Krebs)",
        "Atemwege",
        "Verdauung",
        "Verletzung/Vergiftung",
        "Sonstiges"
    ],
    datasets: [
        {
            label: 'prozentualer Anteil',
            data: [39, 26, 7, 4, 4, 20],
            backgroundColor: [
                "#99ccff",
                "#6699ff",
                "#3366ff",
                "#6622ff",
                "#9966ff",
                "#7575a3"
            ],
            hoverBackgroundColor: [
                "#99ccdd",
                "#6699dd",
                "#3366dd",
                "#6666dd",
                "#9966dd",
                "#757573"
            ],
            borderWidth: 1
        }]
};
death_pie_chart = new Chart(death_pie_container,{
    type: 'pie',
    data: death_pie_data,
    options: {
        scale: {
            scaleLabel: {
                display: true,
                labelString: "Erkrankungen als Todesursache in Deutschland 2014"
            }
        }
    }
});
