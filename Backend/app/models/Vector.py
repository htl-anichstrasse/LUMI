from app.extensions import db


class Vector(db.Model):
    __tablename__ = 'vectors'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    text = db.Column(db.String(255))
    vector = db.Column(db.JSON)
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'))
    user = db.relationship("User", back_populates="vectors")
    
    def __init__(self, text, vector, time, user_id):
        self.text = text
        self.vector = vector
        self.created_at = time
        self.user_id = user_id