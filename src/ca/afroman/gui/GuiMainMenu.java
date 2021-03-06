package ca.afroman.gui;

import ca.afroman.assets.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Texture;
import ca.afroman.client.ClientGame;
import ca.afroman.util.UpdateUtil;
import ca.afroman.util.VersionUtil;

public class GuiMainMenu extends GuiMenuOutline
{
	private boolean isShowingBuild = false;
	
	private boolean allowUpdateCheck;
	
	private GuiTextButton joinButton;
	private GuiTextButton buildButton;
	
	public GuiMainMenu()
	{
		super(null, true, true);
		
		joinButton = new GuiTextButton(this, 1, (ClientGame.WIDTH / 2) - (72 / 2), 58 + (24 * 0), 72, blackFont, "Join");
		buildButton = new GuiTextButton(this, 20, (ClientGame.WIDTH / 2) - (72 / 2), 58 + (24 * 0), 72, blackFont, "Build");
		toggleBuildModeButton(isShowingBuild);
		
		addButton(new GuiTextButton(this, 2, (ClientGame.WIDTH / 2) - (72 / 2), 58 + (24 * 1), 72, blackFont, "Host"));
		addButton(new GuiTextButton(this, 0, (ClientGame.WIDTH / 2) - (72 / 2), 58 + (24 * 2), 72, blackFont, "Quit"));
		addButton(new GuiIconButton(this, 4, (ClientGame.WIDTH / 2) - (72 / 2) - 16 - 4, 58 + (24 * 1), 16, Assets.getStepSpriteAnimation(AssetType.ICON_SETTINGS).clone()));
		addButton(new GuiIconButton(this, 3, (ClientGame.WIDTH / 2) - (72 / 2) - 16 - 4, 58 + (24 * 2), 16, Assets.getStepSpriteAnimation(AssetType.ICON_UPDATE).clone()));
		
		allowUpdateCheck = true;
	}
	
	@Override
	public void drawScreen(Texture renderTo)
	{
		super.drawScreen(renderTo);
		
		nobleFont.renderCentered(renderTo, ClientGame.WIDTH / 2, 15, "The Adventures of Afro Man");
	}
	
	@Override
	public void releaseAction(int buttonID, boolean isLeft)
	{
		// Rids of any pending calls for isReleasedFilter on escape
		ClientGame.instance().input().escape.isReleasedFiltered();
		
		switch (buttonID)
		{
			case 2: // Host Server
				ClientGame.instance().setCurrentScreen(new GuiHostServer(this));
				break;
			case 1: // Join Server
				ClientGame.instance().setCurrentScreen(new GuiJoinServer(this));
				break;
			case 0: // Quit game
				ClientGame.instance().quit();
				break;
			case 3: // Check for updates
				if (allowUpdateCheck) // Prevents the update from being checked if a previous update notification still needs to be closed
				{
					allowUpdateCheck = false;
					if (UpdateUtil.updateQuery())
					{
						new GuiYesNoPrompt(this, 31, "Update found (" + VersionUtil.toString(UpdateUtil.serverVersion) + ")", "Would you like to update?");
					}
					else
					{
						new GuiClickNotification(this, 30, "No updates", "found");
					}
				}
				break;
			case 30: // No updates found, okay?
				allowUpdateCheck = true;
				break;
			case 31: // Confirm update
				new GuiYesNoPrompt(this, 33, "Doing this will close the game,", "Would you like to continue?");
				break;
			case 32: // No, don't update
				allowUpdateCheck = true;
				break;
			case 33:// Yes, update
				ClientGame.instance().quit(true);
				break;
			case 34: // No, don't update
				allowUpdateCheck = true;
				break;
			case 4:// Options menu
				ClientGame.instance().setCurrentScreen(new GuiOptionsMenu(this, false));
				break;
			case 20: // Build mode
				ClientGame.instance().setIsBuildMode(true);
				ClientGame.instance().setIsInGame(true);
				ClientGame.instance().setIsBuildMode(false);
				break;
		}
	}
	
	public void toggleBuildModeButton()
	{
		toggleBuildModeButton(!isShowingBuild);
	}
	
	public void toggleBuildModeButton(boolean isShowing)
	{
		isShowingBuild = isShowing;
		
		if (isShowingBuild)
		{
			addButton(buildButton);
			removeButton(joinButton);
		}
		else
		{
			removeButton(buildButton);
			addButton(joinButton);
		}
	}
}
