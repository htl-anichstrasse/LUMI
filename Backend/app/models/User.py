from app.extensions import db

class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    firstname = db.Column(db.String(255))
    lastname = db.Column(db.String(255))
    username = db.Column(db.String(255))
    email = db.Column(db.String(255), unique=True)
    password = db.Column(db.String(255))
    img = db.Column(db.BLOB)
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    vectors = db.relationship("Vector", back_populates="user")
    costs = db.relationship("CostTracking", back_populates="user")
    geo_data = db.relationship("GeoData", back_populates="user")
    
    def __init__(self, firstname, lastname, username, email, password):
        self.firstname = firstname
        self.lastname = lastname
        self.username = username
        self.email = email
        self.password = password
    
    def __repr__(self):
        return f'<User {self.email}>'