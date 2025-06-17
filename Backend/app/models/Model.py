from app.extensions import db

class Model(db.Model):
    id:int
    name:str
    input_token_cost:float
    output_token_cost:float
    
    __tablename__ = 'models'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.Text, nullable=False)
    input_token_cost = db.Column(db.Float, nullable=False)
    output_token_cost = db.Column(db.Float)
    costs = db.relationship("CostTracking", back_populates="model")
    
    def __init__(self, name, input_token_cost, output_token_cost):
        self.name = name
        self.input_token_cost = input_token_cost
        self.output_token_cost = output_token_cost