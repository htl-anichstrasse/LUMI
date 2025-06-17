const diagramm_color_1 = '#4A90E2';
const diagramm_color_2 = '#50E3C2';
const diagramm_color_3 = '#F5A623';
const diagramm_color_4 = '#D0021B';

function generateCostsChart() {
  document.addEventListener('DOMContentLoaded', function () {
    const ctx = document.getElementById('costs-per-month').getContext('2d');
    // Grouped by month
    const costsChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: costs_per_month.map((cost) => cost.month),
        datasets: [
          {
            label: 'Costs',
            data: costs_per_month.map((cost) => cost.total_cost),
            backgroundColor: `rgba(${parseInt(diagramm_color_1.slice(1, 3), 16)}, ${parseInt(diagramm_color_1.slice(3, 5), 16)}, ${parseInt(diagramm_color_1.slice(5, 7), 16)}, 0.3)`,
            borderColor: diagramm_color_1,
            borderWidth: 1,
            fill: true,
            pointRadius: 0,
          },
        ],
      },
      options: {
        plugins: {
          legend: {
            display: false,
          },
        },
        elements: {
          point: {
            radius: 0,
          },
        },
        scales: {
          y: {
            grid: {
              display: false,
            },
            beginAtZero: true,
          },
        },
      },
    });
    // Kuchendiagramm
    const ctx2 = document.getElementById('costs-per-type').getContext('2d');

    const costsChart2 = new Chart(ctx2, {
      type: 'doughnut',
      data: {
        labels: costs_per_type.map((cost) => cost.type),
        datasets: [
          {
            label: 'Costs',
            data: costs_per_type.map((cost) => cost.amount),
            backgroundColor: [diagramm_color_1, diagramm_color_2, diagramm_color_3],
            borderWidth: 0, // Keine Umrandung
          },
        ],
      },
      options: {
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              color: '#ffffff',
              font: {
                size: 14,
              },
            },
          },
        },
        cutout: '70%',
      },
    });
  });
}

generateCostsChart();
