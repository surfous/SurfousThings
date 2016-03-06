package physicalgraph.device

class HubAction
{
	final String command;

	HubAction(String cmd)
	{
		command = cmd
	}

	String toString()
	{
		return "HA: $command"
	}
}
