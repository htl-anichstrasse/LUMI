from app.extensions import db
import datetime

class CostTracking(db.Model):
    id:int
    tokens:int
    type:str
    created_at:datetime
    user_id:int
    model_id:int
    
    __tablename__ = 'cost_tracking'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    tokens = db.Column(db.Integer)
    type = db.Column(db.String(255))
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    model_id = db.Column(db.Integer, db.ForeignKey('models.id'), nullable=False)
    user = db.relationship("User", back_populates="costs")
    model = db.relationship("Model", back_populates="costs")
    
    def __init__(self, tokens, type, model_id, user_id):
        self.tokens = tokens
        self.type = type
        self.model_id = model_id
        self.user_id = user_id