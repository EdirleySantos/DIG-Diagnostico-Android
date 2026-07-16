package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChessActivity extends Activity {
    private Board board; private final Button[] cells=new Button[64]; private TextView status; private Square selected; private boolean vsCpu=true,ended;
    @Override protected void onCreate(Bundle b){super.onCreate(b);build();newGame();}
    private void build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(8),dp(14),dp(8),dp(8));root.setBackgroundColor(0xffece8dc);TextView title=new TextView(this);title.setText("XADREZ");title.setTextSize(26);title.setTextColor(0xff31364b);root.addView(title);status=new TextView(this);status.setTextSize(15);status.setPadding(0,dp(5),0,dp(8));root.addView(status);GridLayout grid=new GridLayout(this);grid.setColumnCount(8);for(int i=0;i<64;i++){final int index=i;Button c=new Button(this);c.setTextSize(24);c.setPadding(0,0,0,0);c.setMinWidth(0);c.setMinimumWidth(0);c.setOnClickListener(v->tap(index));cells[i]=c;GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(45);lp.columnSpec=GridLayout.spec(i%8,1f);grid.addView(c,lp);}root.addView(grid);Button mode=new Button(this);mode.setText("Modo: contra a maquina");mode.setAllCaps(false);mode.setOnClickListener(v->{vsCpu=!vsCpu;mode.setText(vsCpu?"Modo: contra a maquina":"Modo: dois jogadores");newGame();});root.addView(mode);Button fresh=new Button(this);fresh.setText("Nova partida");fresh.setOnClickListener(v->newGame());root.addView(fresh);setContentView(root);}
    private void newGame(){board=new Board();selected=null;ended=false;render();}
    private void tap(int index){if(ended||(vsCpu&&board.getSideToMove()==Side.BLACK))return;Square sq=square(index);Piece piece=board.getPiece(sq);if(selected==null){if(piece!=Piece.NONE&&piece.getPieceSide()==board.getSideToMove()){selected=sq;status.setText("Escolhida: "+sq);render();}return;}Move legal=findMove(selected,sq);if(legal!=null){board.doMove(legal);selected=null;afterMove();}else{selected=null;status.setText("Movimento invalido");render();}}
    private Move findMove(Square from,Square to){for(Move m:board.legalMoves())if(m.getFrom()==from&&m.getTo()==to)return m;return null;}
    private void afterMove(){render();if(checkEnd())return;if(vsCpu&&board.getSideToMove()==Side.BLACK)status.postDelayed(this::cpuMove,420);}
    private void cpuMove(){List<Move> legal=new ArrayList<>(board.legalMoves());if(legal.isEmpty()){checkEnd();return;}legal.sort(Comparator.comparingInt(this::moveValue).reversed());board.doMove(legal.get(0));afterMove();}
    private int moveValue(Move m){Piece captured=board.getPiece(m.getTo());int v=value(captured);return v*10+(int)(Math.random()*6);}
    private int value(Piece p){String s=p.name();if(s.contains("QUEEN"))return 9;if(s.contains("ROOK"))return 5;if(s.contains("BISHOP")||s.contains("KNIGHT"))return 3;if(s.contains("PAWN"))return 1;return 0;}
    private boolean checkEnd(){if(board.isMated()){boolean win=board.getSideToMove()==Side.BLACK;finishGame(win,win?"Xeque-mate! Voce venceu.":"Xeque-mate. A maquina venceu.");return true;}if(board.isDraw()||board.isStaleMate()){finishGame(false,"Partida empatada.");return true;}status.setText((board.getSideToMove()==Side.WHITE?"Brancas":"Pretas")+" jogam"+(board.isKingAttacked()?" | XEQUE":""));return false;}
    private void finishGame(boolean win,String msg){ended=true;if(win){int old=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0);getSharedPreferences("dig_games",MODE_PRIVATE).edit().putInt("total_points",old+40).apply();}new AlertDialog.Builder(this).setTitle("Fim da partida").setMessage(msg+(win?" +40 pontos":"")).setPositiveButton("Nova partida",(d,w)->newGame()).setNegativeButton("Fechar",null).show();}
    private void render(){for(int i=0;i<64;i++){Square sq=square(i);Button c=cells[i];c.setText(symbol(board.getPiece(sq)));int row=i/8,col=i%8;c.setBackgroundColor(selected==sq?0xffffc857:(row+col)%2==0?0xfff0d9b5:0xff78966b);}}
    private Square square(int i){int row=i/8,col=i%8;return Square.valueOf(""+(char)('A'+col)+(8-row));}
    private String symbol(Piece p){switch(p.name()){case"WHITE_KING":return"♔";case"WHITE_QUEEN":return"♕";case"WHITE_ROOK":return"♖";case"WHITE_BISHOP":return"♗";case"WHITE_KNIGHT":return"♘";case"WHITE_PAWN":return"♙";case"BLACK_KING":return"♚";case"BLACK_QUEEN":return"♛";case"BLACK_ROOK":return"♜";case"BLACK_BISHOP":return"♝";case"BLACK_KNIGHT":return"♞";case"BLACK_PAWN":return"♟";default:return"";}}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
