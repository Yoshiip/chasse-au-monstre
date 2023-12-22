package fr.univlille.multiplayer;

public enum MultiplayerCommand {
  HOST, // the host created the game and is sending its name to its players ("command=0;string")
  JOIN, // a client joined a game and is sending its name to the server ("command=1;string")
  DISCONNECTION, // the client left the lobby ("command=2;integer"), the client sends the hash code of its socket in the communication
  SERVER_TERMINATION, // the host left the lobby
  SET_GAME_ROLES // the players are in the lobby, the host switched the roles (hunter/monster) ("command=..;X"), X being 1 or 0, the value of LobbyModel.host_is_hunter
};
