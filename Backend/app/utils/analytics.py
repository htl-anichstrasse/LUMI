import pandas as pd
import numpy as np
import datetime


def prepareData(costs_db):
    costs = []
    for cost in costs_db:
        new_cost = cost.tokens * cost.model.input_token_cost if cost.type == "input" else cost.tokens * cost.model.output_token_cost
        new_cost_model_name = cost.model.name
        costs.append({"tokens": cost.tokens, "type": cost.type, "created_at": cost.created_at, "amount": new_cost, "model": {"id": cost.model.id, "name": new_cost_model_name}})
    
    df = pd.DataFrame(costs)
    df['model'] = df['model'].apply(lambda x: x['name'])
    return df

def getCostsLastMonths(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
        
    # Costs last 12 months

    df['year'] = df['created_at'].dt.year
    df['month'] = df['created_at'].dt.month

    monthly_costs = df.groupby(['year', 'month'])['amount'].sum().reset_index()

    monthly_costs.rename(columns={'amount': 'total_cost'}, inplace=True)

    monthly_costs.sort_values(by=['year', 'month'], inplace=True, ascending=False)

    # 1. Bestimme den Jahrbereich und den aktuellen Monat
    current_year = datetime.datetime.now().year
    current_month = datetime.datetime.now().month

    # 2. Erstelle eine vollständige Liste aller Monate bis zum aktuellen Monat
    all_months = pd.DataFrame([
        (year, month) 
        for year in range(df['year'].min(), current_year + 1) 
        for month in range(1, 13)
        if not (year == current_year and month > current_month)  # Nur bis zum aktuellen Monat
    ], columns=['year', 'month'])

    # 3. Identifiziere die fehlenden Monate durch einen Left Join
    missing_months = pd.merge(all_months, monthly_costs, on=['year', 'month'], how='left', indicator=True)
    missing_months = missing_months[missing_months['_merge'] == 'left_only'][['year', 'month']]

    # 4. Setze total_cost für fehlende Monate auf 0
    missing_months['total_cost'] = 0

    # 5. Füge die fehlenden Monate in den originalen DataFrame ein
    monthly_costs_complete = pd.concat([monthly_costs, missing_months]).sort_values(by=['year', 'month']).reset_index(drop=True)

    monthly_costs_complete.sort_values(by=['year', 'month'], inplace=True, ascending=False)

    return monthly_costs_complete.head(12).sort_values(by=['year', 'month'], ascending=True)

def getCostsByModel(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
        
    # Costs per model
    model_costs = df.groupby(['model'])['amount'].sum().reset_index()
    model_costs.sort_values(by=['amount'], inplace=True, ascending=False)
    
    return model_costs

def getCostsByType(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
    
    # Costs per type
    type_costs = df.groupby(['type'])['amount'].sum().reset_index()
    type_costs.sort_values(by=['amount'], inplace=True, ascending=False)
    
    return type_costs


def getCostsByModelAndType(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
    
    # Costs per model and type
    model_type_costs = df.groupby(['model', 'type'])['amount'].sum().reset_index()
    model_type_costs.sort_values(by=['amount'], inplace=True, ascending=False)
    
    return model_type_costs

def getTotalCosts(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
    
    return df['amount'].sum()

def getAvgCostsPerMonth(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
    
    return df['amount'].mean()

def getAvgCostsPerDay(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
    
    return df['amount'].mean() / 30

def getCostsLastMonth(costs, prepared=False):
    df = None
    if not prepared:
        df = prepareData(costs)
    else:
        df = pd.DataFrame(costs)
        df['model'] = df['model'].apply(lambda x: x['name'])
    
    last_month = datetime.datetime.now().month - 1 if datetime.datetime.now().month != 1 else 12
    last_year = datetime.datetime.now().year if datetime.datetime.now().month != 1 else datetime.datetime.now().year - 1
    
    return df[(df['created_at'].dt.month == last_month) & (df['created_at'].dt.year == last_year)]['amount'].sum()

test_data = [{'tokens': 7, 'type': 'input', 'created_at': datetime.datetime(2023, 10, 28, 13, 42, 8), 'amount': 1.4000000000000001e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 12, 'type': 'input', 'created_at': datetime.datetime(2024, 1, 28, 13, 42, 9), 'amount': 2.4000000000000003e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 11, 'type': 'input', 'created_at': datetime.datetime(2023, 12, 28, 13, 42, 9), 'amount': 2.2000000000000002e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 11, 'type': 'input', 'created_at': datetime.datetime(2024, 8, 28, 13, 42, 10), 'amount': 2.2000000000000002e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 9, 'type': 'input', 'created_at': datetime.datetime(2024, 10, 28, 13, 42, 10), 'amount': 1.8000000000000002e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 15, 'type': 'input', 'created_at': datetime.datetime(2024, 10, 28, 13, 42, 12), 'amount': 3.0000000000000004e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 7, 'type': 'input', 'created_at': datetime.datetime(2024, 10, 28, 13, 42, 12), 'amount': 1.4000000000000001e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 9, 'type': 'input', 'created_at': datetime.datetime(2024, 10, 28, 13, 42, 13), 'amount': 1.8000000000000002e-08, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 2, 'type': 'input', 'created_at': datetime.datetime(2024, 11, 7, 20, 20, 25), 'amount': 4e-09, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}, {'tokens': 2, 'type': 'input', 'created_at': datetime.datetime(2024, 11, 7, 20, 40, 56), 'amount': 4e-09, 'model': {'id': 2, 'name': 'text-embedding-3-small'}}]





if __name__ == "__main__":
    print(getCostsLastMonths(test_data, prepared=True))
    print("\n\n\n")
    print(getCostsByModel(test_data, prepared=True))
    print("\n\n\n")
    print(getCostsByType(test_data, prepared=True))
    print("\n\n\n")
    print(getCostsByModelAndType(test_data, prepared=True))
    print("\n\n\n")
    print(getTotalCosts(test_data, prepared=True))
    print("\n\n\n")
    print(getAvgCostsPerMonth(test_data, prepared=True))
    print("\n\n\n")
    print(getAvgCostsPerDay(test_data, prepared=True))
    print("\n\n\n")
    print(getCostsLastMonth(test_data, prepared=True))
    print("\n\n\n")