from app.extensions import db

class StopLocation(db.Model):
    id:int
    name:str
    extId:str
    
    __tablename__ = 'stop_locations'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    name = db.Column(db.Text, nullable=False)
    extId = db.Column(db.Text, nullable=False)
    
    def __init__(self, name, extId):
        self.name = name
        self.extId = extId