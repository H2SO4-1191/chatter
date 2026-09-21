const admin = require("firebase-admin");
const express = require("express");
const app = express();
const bodyParser = require("body-parser");
const serviceAccount = require("./firebase-service-account.json");
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});
function sendNotification(token, title, message) {
  const messagePayload = {
    notification: {
      title: title,
      body: message,
    },
    token: token,
  };
  admin
    .messaging()
    .send(messagePayload)
    .then((response) => {
      console.log("Notification sent successfully:", response);
    })
    .catch((error) => {
      console.error("Error sending notification:", error);
    });
}
app.use(bodyParser.json());
app.post("/sendNotification", (req, res) => {
  const { token, title, message } = req.body;
  sendNotification(token, title, message);
  res.status(200).send("Notification sent");
});
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
