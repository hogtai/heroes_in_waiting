const jwtConfig = {
    secret: process.env.JWT_SECRET,
    expiresIn: '1h',
    issuer: 'heroes-in-waiting',
    audience: 'heroes-in-waiting-app',
    algorithm: 'HS256',
    clockTolerance: 30,
    maxAge: 3600000 // 1 hour in milliseconds
};

module.exports = jwtConfig; 