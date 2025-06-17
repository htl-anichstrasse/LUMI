from flask import Blueprint, render_template

from app.models import CostTracking
from app.extensions import db
from app.utils import getCostsByModel, getCostsByType, getCostsByModelAndType, getTotalCosts, getCostsLastMonth, getAvgCostsPerMonth, getAvgCostsPerDay, getCostsLastMonths

# Erstelle den Blueprint
costs_bp = Blueprint('costs', __name__)

@costs_bp.route('/')
def costs():
    costs_db = db.session.query(CostTracking).all()
    return render_template('costs.html', 
                           costs_per_month=getCostsLastMonths(costs_db).to_dict(orient='records'), # Liniendiagramm
                           costs_per_model=getCostsByModel(costs_db).to_dict(orient='records'), # Balkendiagramm
                           costs_per_type=getCostsByType(costs_db).to_dict(orient='records'), # Tortendiagramm
                            costs_per_model_and_type=getCostsByModelAndType(costs_db).to_dict(orient='records'), # Gruppiertes Balkendiagramm
                            total_costs=f'{getTotalCosts(costs_db).round(10):.1e}',
                            costs_last_month=f'{getCostsLastMonth(costs_db).round(10):.1e}' ,
                            avg_costs_per_month=f'{getAvgCostsPerMonth(costs_db).round(10):.1e}',
                            avg_costs_per_day=f'{getAvgCostsPerDay(costs_db).round(10):.1e}'
                           )

# Vorlage: https://cdn.prod.website-files.com/5efb0b7816032fd33ce6059c/62d5bbc98f1e1333116cd204_Screen%20Shot%202021-10-07%20at%2012.36.png

# Kasten:
# total costs
# costs last month
# avg. costs per month
# avg. costs per day

# Graphen:
# Costs per month
# Costs per model
# Costs per type
# Costs per model and type
