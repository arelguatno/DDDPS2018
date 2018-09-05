const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
// Create and Deploy Your First Cloud Functions
// https://firebase.google.com/docs/functions/write-firebase-functions

exports.createUsers = functions.region('asia-northeast1').auth.user().onCreate((user) => {
    console.log(user);

    userDoc = {'email': user.email, 
               'displayName' : user.displayName,
               'emailVerified': user.emailVerified,
                'dateCreation': Date.now()}

    admin.firestore().collection('users').doc(user.uid)
    .set(userDoc).then(writeResult => {
        console.log('createUsers: Created', writeResult);
        return;
    }).catch(err => {
       console.log(err);
       return;
    });
});

exports.createAccountProfile = functions.region('asia-northeast1').auth.user().onCreate((user) => {
    console.log(user);

    userDoc = {'balance': 100.00}

    admin.firestore().collection('account_profile').doc(user.uid)
    .set(userDoc).then(writeResult => {
        console.log('createAccountProfile: Created ', writeResult);
        return;
    }).catch(err => {
       console.log(err);
       return;
    });
});

exports.createAccountHistory = functions.region('asia-northeast1').auth.user().onCreate((user) => {
    console.log(user);

    userDoc = {'amount': 100.00,
               'source' : 'Auto Generated',  // Either Counter or Kioks
               'source_location' : 'Auto Generated',  // e.g MRT Ayala, North
               'date' : Date.now(),
               'description' : 'Account Created',
               'type' : 'na'   // Credit (minus), TopUp (plus)
              }

    admin.firestore().collection('account_history').doc(user.uid).collection('data').doc()
    .set(userDoc).then(writeResult => {
        console.log('createAccountHistory: Created ', writeResult);
        return;
    }).catch(err => {
       console.log(err);
       return;
    });
});


