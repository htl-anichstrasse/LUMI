from app.extensions import db
import datetime

class GeoData(db.Model):
    id: int
    data: str
    created_at: datetime
    user_id: int
    
    __tablename__ = 'geo_data'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    data = db.Column(db.String(255))
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
    user = db.relationship("User", back_populates="geo_data")
    
    def __init__(self, data, user_id):
        self.data = data
        self.user_id = user_id