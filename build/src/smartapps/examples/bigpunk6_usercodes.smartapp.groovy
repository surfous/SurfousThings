/**
* Change Lock Codes
*
* Author: bigpunk6
*/


// Automatically generated. Make future change here.
definition(
	name: "bigpunk lock usercodes",
	namespace: "bigpunk6",
	author: "Big Punk",
	description: "nada",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("What Lock")
	{
		input "lock1","capability.lock", title: "Lock"
	}

	section("User")
	{
		input "user1", "decimal", title: "User (From 1 to 30) "
		input "code1", "decimal", title: "Code (4 to 8 digits)"
		input "delete1", "enum", title: "Delete User", required: false, metadata: [values: ["Yes","No"]]
	}
}

def installed()
{
		subscribe(app, appTouch)
		subscribe(lock1, "codeReport", usercodeget)
}

def updated()
{
		unsubscribe()
		subscribe(app, appTouch)
		subscribe(lock1, "codeReport", usercodeget)
}

def appTouch(evt)
{
	log.debug "Current Code for user $user1: $lock1.currentUsercode"
	log.debug "user: $user1, code: $code1"
	def idstatus1 = 1
	if (delete1 == "Yes")
    {
        lock1.deleteCode(user1)
	}
    else
    {
		lock1.setCode(user1, code1, idstatus1)
	}
}

def usercodeget(evt){
	log.debug "Current Code for user $user1: $lock1.currentUsercode"
}
